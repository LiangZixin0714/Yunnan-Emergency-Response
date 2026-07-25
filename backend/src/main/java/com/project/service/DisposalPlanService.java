package com.project.service;

import com.project.annotation.SystemAuditLog;
import com.project.entity.mysql.Incident;
import com.project.entity.mysql.Plan;
import com.project.repository.mysql.IncidentRepository;
import com.project.repository.mysql.PlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DisposalPlanService {

    private static final Logger logger = LoggerFactory.getLogger(DisposalPlanService.class);

    private final PlanRepository planRepository;
    private final IncidentRepository incidentRepository;

    public DisposalPlanService(PlanRepository planRepository, IncidentRepository incidentRepository) {
        this.planRepository = planRepository;
        this.incidentRepository = incidentRepository;
    }

    public List<Plan> listByIncidentId(String incidentId) {
        return planRepository.findByIncidentId(incidentId);
    }

    public Plan saveDraft(Long id, String planContent, String incidentId) {
        Plan plan;
        if (id != null) {
            plan = planRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("方案不存在，ID: " + id));
            plan.setPlanContent(planContent);
            plan.setStatus("draft");
            plan = planRepository.save(plan);
        } else {
            plan = new Plan();
            plan.setPlanId("DP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            plan.setIncidentId(incidentId);
            plan.setPlanTitle("处置方案草稿");
            plan.setPlanContent(planContent);
            plan.setStatus("draft");
            plan = planRepository.save(plan);
        }

        updateIncidentDisposalPlanStatus(plan.getIncidentId(), "draft");
        return plan;
    }

    @Transactional
    public Plan submit(Long id, String planContent, String incidentId, Long submittedBy) {
        Plan plan;
        if (id != null) {
            plan = planRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("方案不存在，ID: " + id));
            plan.setPlanContent(planContent);
        } else {
            plan = new Plan();
            plan.setPlanId("DP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            plan.setIncidentId(incidentId);
            plan.setPlanTitle("处置方案");
            plan.setPlanContent(planContent);
        }
        plan.setStatus("submitted");
        plan.setSubmittedBy(submittedBy);
        plan.setSubmittedAt(LocalDateTime.now());
        plan.setRejectReason(null);
        plan = planRepository.save(plan);

        updateIncidentDisposalPlanStatus(plan.getIncidentId(), "submitted");
        logger.info("处置方案已提交，planId: {}, incidentId: {}", plan.getPlanId(), plan.getIncidentId());
        return plan;
    }

    @SystemAuditLog(module = "disposal-plan", action = "reject")
    @Transactional
    public Plan reject(Long id, String rejectReason, String incidentId) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("方案不存在，ID: " + id));

        plan.setStatus("rejected");
        plan.setRejectReason(rejectReason);
        plan = planRepository.save(plan);

        updateIncidentDisposalPlanStatus(plan.getIncidentId(), "rejected");
        updateIncidentResourceDispatchStatus(plan.getIncidentId(), "shortage");

        logger.info("处置方案已驳回，planId: {}, incidentId: {}", plan.getPlanId(), plan.getIncidentId());
        return plan;
    }

    @Transactional
    public Plan accept(Long id, String incidentId) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("方案不存在，ID: " + id));

        plan.setStatus("accepted");
        plan = planRepository.save(plan);

        updateIncidentDisposalPlanStatus(plan.getIncidentId(), "accepted");
        logger.info("处置方案已接受，planId: {}, incidentId: {}", plan.getPlanId(), plan.getIncidentId());
        return plan;
    }

    private void updateIncidentDisposalPlanStatus(String incidentId, String status) {
        Incident incident = incidentRepository.findByIncidentId(incidentId).orElse(null);
        if (incident != null) {
            incident.setDisposalPlanStatus(status);
            incidentRepository.save(incident);
            logger.debug("更新灾情处置方案状态，incidentId: {}, status: {}", incidentId, status);
        }
    }

    private void updateIncidentResourceDispatchStatus(String incidentId, String status) {
        Incident incident = incidentRepository.findByIncidentId(incidentId).orElse(null);
        if (incident != null) {
            incident.setResourceDispatchStatus(status);
            incidentRepository.save(incident);
            logger.debug("更新灾情资源调度状态，incidentId: {}, status: {}", incidentId, status);
        }
    }
}
