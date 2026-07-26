package com.project.service;

import com.project.annotation.SystemAuditLog;
import com.project.entity.mysql.EmergencyResource;
import com.project.entity.mysql.Incident;
import com.project.entity.mysql.ResourceRequest;
import com.project.repository.mysql.EmergencyResourceRepository;
import com.project.repository.mysql.IncidentRepository;
import com.project.repository.mysql.ResourceRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ResourceRequestService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceRequestService.class);

    private final ResourceRequestRepository resourceRequestRepository;
    private final IncidentRepository incidentRepository;
    private final EmergencyResourceRepository resourceRepository;

    public ResourceRequestService(ResourceRequestRepository resourceRequestRepository,
                                  IncidentRepository incidentRepository,
                                  EmergencyResourceRepository resourceRepository) {
        this.resourceRequestRepository = resourceRequestRepository;
        this.incidentRepository = incidentRepository;
        this.resourceRepository = resourceRepository;
    }

    public List<ResourceRequest> listByIncidentId(String incidentId) {
        return resourceRequestRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId);
    }

    public List<ResourceRequest> listPending() {
        return resourceRequestRepository.findAll().stream()
                .filter(r -> "pending".equals(r.getStatus()))
                .toList();
    }

    @SystemAuditLog(module = "resource-request", action = "submit")
    @Transactional
    public List<ResourceRequest> submitBatch(String incidentId, List<ResourceRequestItem> items, Long requesterId) {
        if (incidentId == null || incidentId.isEmpty()) {
            throw new IllegalArgumentException("incidentId不能为空");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("资源申请项不能为空");
        }

        List<ResourceRequest> requests = new ArrayList<>();
        for (ResourceRequestItem item : items) {
            ResourceRequest request = new ResourceRequest();
            request.setRequestId("RR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            request.setIncidentId(incidentId);
            request.setResourceId(item.getResourceId());
            request.setResourceName(item.getResourceName());
            request.setResourceType(item.getResourceType());
            request.setQuantity(item.getQuantity());
            request.setUnit(item.getUnit());
            request.setPriority(item.getPriority());
            request.setDestination(item.getDestination());
            request.setStatus("pending");
            request.setRemark(item.getRemark());
            request.setRequesterId(requesterId);
            request = resourceRequestRepository.save(request);
            requests.add(request);
        }

        updateIncidentResourceDispatchStatus(incidentId, "shortage");
        logger.info("批量提交资源申请，incidentId: {}, 数量: {}", incidentId, items.size());
        return requests;
    }

    @SystemAuditLog(module = "resource-request", action = "approve")
    @Transactional
    public ResourceRequest approve(Long id) {
        ResourceRequest request = resourceRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("资源申请不存在，id: " + id));

        if (!"pending".equals(request.getStatus())) {
            throw new IllegalArgumentException("该申请已处理，状态: " + request.getStatus());
        }

        final String resourceIdStr = request.getResourceId();
        EmergencyResource resource = resourceRepository.findByResourceId(resourceIdStr)
                .orElseThrow(() -> new IllegalArgumentException("资源不存在: " + resourceIdStr));

        int qty = request.getQuantity() != null ? request.getQuantity() : 0;
        resource.setTotalStock((resource.getTotalStock() != null ? resource.getTotalStock() : 0) + qty);
        resource.setAvailableStock((resource.getAvailableStock() != null ? resource.getAvailableStock() : 0) + qty);
        resourceRepository.save(resource);

        request.setStatus("approved");
        request = resourceRequestRepository.save(request);

        updateIncidentResourceDispatchStatus(request.getIncidentId(), "executing");
        logger.info("资源申请已批准，id: {}, resourceId: {}, quantity: {}", id, resourceIdStr, qty);
        return request;
    }

    @SystemAuditLog(module = "resource-request", action = "reject")
    @Transactional
    public ResourceRequest reject(Long id, String reason) {
        ResourceRequest request = resourceRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("资源申请不存在，id: " + id));

        if (!"pending".equals(request.getStatus())) {
            throw new IllegalArgumentException("该申请已处理，状态: " + request.getStatus());
        }

        request.setStatus("rejected");
        request.setRemark(reason != null ? reason : "驳回");
        request = resourceRequestRepository.save(request);

        updateIncidentDisposalPlanStatus(request.getIncidentId(), "rejected");
        updateIncidentResourceDispatchStatus(request.getIncidentId(), "shortage");
        logger.info("资源申请已驳回，id: {}", id);
        return request;
    }

    private void updateIncidentResourceDispatchStatus(String incidentId, String status) {
        Incident incident = incidentRepository.findByIncidentId(incidentId).orElse(null);
        if (incident != null) {
            incident.setResourceDispatchStatus(status);
            incidentRepository.save(incident);
            logger.debug("更新灾情资源调度状态，incidentId: {}, status: {}", incidentId, status);
        }
    }

    private void updateIncidentDisposalPlanStatus(String incidentId, String status) {
        Incident incident = incidentRepository.findByIncidentId(incidentId).orElse(null);
        if (incident != null) {
            incident.setDisposalPlanStatus(status);
            incidentRepository.save(incident);
            logger.debug("更新灾情处置方案状态，incidentId: {}, status: {}", incidentId, status);
        }
    }

    public static class ResourceRequestItem {
        private String resourceId;
        private String resourceName;
        private String resourceType;
        private Integer quantity;
        private String unit;
        private String priority;
        private String destination;
        private String remark;

        public String getResourceId() { return resourceId; }
        public void setResourceId(String resourceId) { this.resourceId = resourceId; }
        public String getResourceName() { return resourceName; }
        public void setResourceName(String resourceName) { this.resourceName = resourceName; }
        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}
