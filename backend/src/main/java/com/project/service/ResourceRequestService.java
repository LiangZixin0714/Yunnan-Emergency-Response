package com.project.service;

import com.project.annotation.SystemAuditLog;
import com.project.entity.mysql.Incident;
import com.project.entity.mysql.ResourceRequest;
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

    public ResourceRequestService(ResourceRequestRepository resourceRequestRepository,
                                  IncidentRepository incidentRepository) {
        this.resourceRequestRepository = resourceRequestRepository;
        this.incidentRepository = incidentRepository;
    }

    public List<ResourceRequest> listByIncidentId(String incidentId) {
        return resourceRequestRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId);
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

        updateIncidentResourceDispatchStatus(incidentId, "executing");
        logger.info("批量提交资源申请，incidentId: {}, 数量: {}", incidentId, items.size());
        return requests;
    }

    private void updateIncidentResourceDispatchStatus(String incidentId, String status) {
        Incident incident = incidentRepository.findByIncidentId(incidentId).orElse(null);
        if (incident != null) {
            incident.setResourceDispatchStatus(status);
            incidentRepository.save(incident);
            logger.debug("更新灾情资源调度状态，incidentId: {}, status: {}", incidentId, status);
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
