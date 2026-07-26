package com.project.service;

import com.project.config.AiServiceConfig;
import com.project.entity.mysql.Knowledge;
import com.project.repository.mysql.KnowledgeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class VectorizeService {

    private static final Logger logger = LoggerFactory.getLogger(VectorizeService.class);

    private final RestTemplate restTemplate;
    private final AiServiceConfig aiServiceConfig;
    private final KnowledgeRepository knowledgeRepository;

    public VectorizeService(RestTemplate restTemplate, AiServiceConfig aiServiceConfig,
                           KnowledgeRepository knowledgeRepository) {
        this.restTemplate = restTemplate;
        this.aiServiceConfig = aiServiceConfig;
        this.knowledgeRepository = knowledgeRepository;
    }

    @Async
    public void triggerVectorizeAsync(String fileId) {
        doVectorize(fileId);
    }

    public void triggerVectorizeWithRetryCheck(String fileId) {
        doVectorize(fileId);
    }

    private void doVectorize(String fileId) {
        Knowledge knowledge = knowledgeRepository.findByFileId(fileId).orElse(null);
        if (knowledge == null) {
            logger.error("向量化触发失败：文件不存在, fileId={}", fileId);
            return;
        }

        knowledge.setVectorizeStatus(Knowledge.STATUS_PROCESSING);
        knowledge.setVectorizeStartedAt(LocalDateTime.now());
        knowledgeRepository.save(knowledge);

        try {
            String url = aiServiceConfig.getUrl() + "/api/v1/knowledge/vectorize";
            Map<String, Object> requestBody = Map.of(
                    "objectKey", knowledge.getObjectKey(),
                    "bucket", knowledge.getBucket(),
                    "fileId", knowledge.getFileId(),
                    "fileName", knowledge.getFileName()
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

            if (response != null && "completed".equals(response.get("status"))) {
                knowledge.setVectorizeStatus(Knowledge.STATUS_COMPLETED);
                knowledge.setVectorizeCompletedAt(LocalDateTime.now());
                knowledge.setChunkCount(response.get("chunkCount") != null ? ((Number) response.get("chunkCount")).intValue() : 0);
                knowledge.setVectorizeFailReason(null);
                logger.info("向量化完成: fileId={}, chunkCount={}", fileId, knowledge.getChunkCount());
            } else {
                String failReason = response != null ? (String) response.get("failReason") : "未知错误";
                handleVectorizeFailure(knowledge, failReason);
            }
        } catch (RestClientException e) {
            logger.error("向量化请求失败: fileId={}, error={}", fileId, e.getMessage());
            handleVectorizeFailure(knowledge, "AI服务不可达");
        } catch (Exception e) {
            logger.error("向量化异常: fileId={}", fileId, e);
            handleVectorizeFailure(knowledge, "向量化处理异常: " + e.getMessage());
        }

        knowledgeRepository.save(knowledge);
    }

    private void handleVectorizeFailure(Knowledge knowledge, String failReason) {
        knowledge.setVectorizeStatus(Knowledge.STATUS_FAILED);
        knowledge.setVectorizeCompletedAt(LocalDateTime.now());
        knowledge.setVectorizeFailReason(failReason);
        knowledge.setVectorizeRetryCount(knowledge.getVectorizeRetryCount() + 1);
        logger.warn("向量化失败: fileId={}, reason={}, retryCount={}",
                knowledge.getFileId(), failReason, knowledge.getVectorizeRetryCount());
    }

    public boolean deleteVectors(String fileName) {
        try {
            String url = aiServiceConfig.getUrl() + "/api/v1/knowledge/vectors/delete";
            Map<String, Object> requestBody = Map.of("sourceFile", fileName);

            restTemplate.postForObject(url, requestBody, Map.class);
            logger.info("向量删除请求成功: fileName={}", fileName);
            return true;
        } catch (Exception e) {
            logger.warn("向量删除失败（不阻断文件删除）: fileName={}, error={}", fileName, e.getMessage());
            return false;
        }
    }
}