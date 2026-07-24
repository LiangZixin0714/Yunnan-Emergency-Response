package com.project.service;

import com.project.annotation.SystemAuditLog;
import com.project.entity.mysql.Incident;
import com.project.entity.mysql.Plan;
import com.project.repository.mysql.IncidentRepository;
import com.project.repository.mysql.PlanRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class PlanService {

    private static final Logger logger = LoggerFactory.getLogger(PlanService.class);
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    
    private static final String AI_SERVICE_BASE_URL = "http://localhost:8002";
    private static final String AI_SYNC_API = "/api/v1/generate-plan";
    private static final String AI_STREAM_API = "/api/v1/generate-plan/stream";
    private static final int SSE_CHUNK_SIZE = 10;
    private static final int SSE_SLEEP_MS = 50;

    private final PlanRepository planRepository;
    private final IncidentRepository incidentRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PlanService(PlanRepository planRepository, IncidentRepository incidentRepository) {
        this.planRepository = planRepository;
        this.incidentRepository = incidentRepository;
        this.restClient = RestClient.create(AI_SERVICE_BASE_URL);
        this.objectMapper = new ObjectMapper();
    }

    @Transactional("mysqlTransactionManager")
    public Map<String, String> generatePlan(String incidentId) {
        Incident incident = incidentRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("灾情不存在"));

        String planContent;
        try {
            planContent = callSyncApi(incident);
        } catch (Exception e) {
            logger.error("调用外部API失败，使用Mock数据", e);
            planContent = generateMockPlanContent(incident);
        }

        String planId = UUID.randomUUID().toString();
        
        Plan plan = new Plan();
        plan.setPlanId(planId);
        plan.setIncidentId(incidentId);
        plan.setPlanTitle("应急预案 - " + incident.getIncidentName());
        plan.setPlanContent(planContent);
        plan.setStatus("draft");

        planRepository.save(plan);

        Map<String, String> result = new HashMap<>();
        result.put("planId", planId);
        return result;
    }

    @Transactional("mysqlTransactionManager")
    public Plan getPlanById(String planId) {
        return planRepository.findByPlanId(planId)
                .orElseThrow(() -> new IllegalArgumentException("方案不存在"));
    }

    public List<Plan> getPlansByIncidentId(String incidentId) {
        return planRepository.findByIncidentId(incidentId);
    }

    @Transactional("mysqlTransactionManager")
    @SystemAuditLog(module = "plan", action = "review", actionType = "UPDATE")
    public Plan reviewPlan(String planId, String action, String modifyContent, String remark) {
        Plan plan = planRepository.findByPlanId(planId)
                .orElseThrow(() -> new IllegalArgumentException("方案不存在"));

        switch (action.toUpperCase()) {
            case "APPROVE":
                plan.setStatus("approved");
                break;
            case "REJECT":
                plan.setStatus("rejected");
                break;
            case "MODIFY":
                if (modifyContent != null && !modifyContent.isEmpty()) {
                    plan.setPlanContent(modifyContent);
                }
                plan.setStatus("modified");
                break;
            default:
                throw new IllegalArgumentException("无效的审核操作: " + action);
        }

        planRepository.save(plan);
        logger.info("方案审核完成，planId: {}, action: {}, status: {}", planId, action, plan.getStatus());
        return plan;
    }

    public SseEmitter streamPlan(String incidentId) {
        SseEmitter emitter = new SseEmitter(300000L);

        executorService.execute(() -> {
            Incident incident = incidentRepository.findByIncidentId(incidentId).orElse(null);
            String description = buildDescription(incident);

            try {
                logger.info("开始生成应急预案，incidentId: {}", incidentId);
                logger.info("灾情描述: {}", description);

                String planContent;
                try {
                    planContent = callSyncApi(incident);
                    logger.info("AI服务返回方案长度: {}", planContent != null ? planContent.length() : 0);
                } catch (Exception e) {
                    logger.error("调用AI服务失败，使用Mock数据", e);
                    planContent = generateMockPlanContent(incident);
                }

                if (planContent == null || planContent.isEmpty()) {
                    logger.error("方案内容为空");
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("生成应急预案失败，请稍后重试"));
                    emitter.complete();
                    return;
                }

                char[] chars = planContent.toCharArray();
                for (int i = 0; i < chars.length; i++) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(String.valueOf(chars[i])));
                        Thread.sleep(SSE_SLEEP_MS);
                    } catch (IOException e) {
                        logger.error("发送SSE数据失败", e);
                        break;
                    }
                }

                emitter.complete();
                logger.info("SSE stream completed for incident: {}", incidentId);

            } catch (Exception e) {
                logger.error("生成应急预案失败，incidentId: {}", incidentId, e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("生成应急预案失败，请稍后重试"));
                } catch (IOException ioEx) {
                    logger.error("发送错误消息失败", ioEx);
                }
                emitter.complete();
            }
        });

        emitter.onCompletion(() -> logger.info("SSE emitter completed"));
        emitter.onTimeout(() -> logger.warn("SSE emitter timeout"));
        emitter.onError(e -> logger.error("SSE emitter error", e));

        return emitter;
    }

    private String callSyncApi(Incident incident) throws Exception {
        String description = buildDescription(incident);
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("description", description);

        String response = restClient.post()
                .uri(AI_SYNC_API)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        JsonNode rootNode = objectMapper.readTree(response);
        JsonNode planNode = rootNode.get("plan");
        
        if (planNode != null && !planNode.isNull()) {
            return planNode.asText();
        }
        
        logger.warn("外部API返回结果中未找到plan字段");
        return null;
    }

    private String buildDescription(Incident incident) {
        if (incident == null) {
            return "灾情信息";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("灾害类型：").append(incident.getDisasterType());
        
        if (incident.getLocation() != null && !incident.getLocation().isEmpty()) {
            sb.append("，发生地点：").append(incident.getLocation());
        }
        
        if (incident.getDescription() != null && !incident.getDescription().isEmpty()) {
            sb.append("，灾情描述：").append(incident.getDescription());
        }
        
        if (incident.getIncidentLevel() != null && !incident.getIncidentLevel().isEmpty()) {
            sb.append("，灾情级别：").append(incident.getIncidentLevel());
        }
        
        return sb.toString();
    }

    private String generateMockPlanContent(Incident incident) {
        String disasterType = incident != null ? incident.getDisasterType() : "灾害";
        String location = incident != null && incident.getLocation() != null ? incident.getLocation() : "事发地点";

        return """
            【应急预案】

            一、灾情概况
            灾害类型：""" + disasterType + """
            发生地点：""" + location + """

            二、响应级别
            根据灾情严重程度，启动三级响应。

            三、组织指挥体系
            成立应急指挥部，统一指挥协调救援工作。

            四、应急处置措施
            1. 立即组织人员疏散，确保群众生命安全
            2. 调配救援物资和设备，保障救援需求
            3. 开展现场搜救，全力抢救被困人员
            4. 设置临时安置点，妥善安置受灾群众
            5. 加强卫生防疫，防止次生灾害发生

            五、资源保障
            - 救援队伍：消防、武警、专业救援队
            - 医疗物资：急救药品、医疗器械
            - 运输车辆：救护车、物资运输车
            - 通讯设备：卫星电话、对讲机

            六、信息报告与发布
            及时向相关部门报告灾情，统一发布信息。

            七、注意事项
            1. 救援人员注意自身安全
            2. 严格执行应急预案流程
            3. 保持信息畅通，及时反馈进展
            """;
    }
}