package com.project.service;

import com.project.annotation.SystemAuditLog;
import com.project.entity.mysql.EmergencyResource;
import com.project.entity.mysql.Incident;
import com.project.entity.mysql.Plan;
import com.project.entity.mysql.ResourceDispatchRecord;
import com.project.repository.mysql.EmergencyResourceRepository;
import com.project.repository.mysql.IncidentRepository;
import com.project.repository.mysql.PlanRepository;
import com.project.repository.mysql.ResourceDispatchRecordRepository;
import com.project.util.RedisLockUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ResourceService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceService.class);

    private final EmergencyResourceRepository resourceRepository;
    private final ResourceDispatchRecordRepository dispatchRecordRepository;
    private final IncidentRepository incidentRepository;
    private final PlanRepository planRepository;
    private final RedisLockUtil redisLockUtil;

    @PersistenceContext(unitName = "mysql")
    private EntityManager entityManager;

    public ResourceService(EmergencyResourceRepository resourceRepository,
                          ResourceDispatchRecordRepository dispatchRecordRepository,
                          IncidentRepository incidentRepository,
                          PlanRepository planRepository,
                          RedisLockUtil redisLockUtil) {
        this.resourceRepository = resourceRepository;
        this.dispatchRecordRepository = dispatchRecordRepository;
        this.incidentRepository = incidentRepository;
        this.planRepository = planRepository;
        this.redisLockUtil = redisLockUtil;
    }

    @Transactional(readOnly = true, transactionManager = "mysqlTransactionManager")
    public List<EmergencyResource> getAvailableResources() {
        return resourceRepository.findByAvailableStockGreaterThan(0);
    }

    @Transactional(readOnly = true, transactionManager = "mysqlTransactionManager")
    public List<EmergencyResource> getAllResources() {
        entityManager.clear();
        return resourceRepository.findAll();
    }

    @Transactional("mysqlTransactionManager")
    @SystemAuditLog(module = "resource", action = "lock", actionType = "UPDATE")
    public Map<String, Object> lockResource(String resourceId, Integer quantity, String incidentId, String planId, String remark) {
        String lockKey = "resource:" + resourceId;
        
        if (!redisLockUtil.lock(lockKey)) {
            throw new RuntimeException("资源正在被其他操作占用，请稍后重试");
        }

        try {
            if (incidentId != null && !incidentId.isEmpty()) {
                boolean incidentCompleted = incidentRepository.findByIncidentId(incidentId)
                        .map(inc -> "completed".equals(inc.getStatus()))
                        .orElse(false);
                if (incidentCompleted) {
                    throw new IllegalArgumentException("已结束的事件不可再调度资源");
                }
            }

            EmergencyResource resource = resourceRepository.findByResourceId(resourceId)
                    .orElseThrow(() -> new IllegalArgumentException("资源不存在"));

            if (resource.getAvailableStock() < quantity) {
                throw new IllegalArgumentException("可用库存不足，当前可用: " + resource.getAvailableStock());
            }

            int updated = resourceRepository.lockResource(resourceId, quantity);
            if (updated == 0) {
                throw new RuntimeException("锁定资源失败");
            }

            entityManager.flush();
            entityManager.clear();

            resource = resourceRepository.findByResourceId(resourceId)
                    .orElseThrow(() -> new IllegalArgumentException("资源不存在"));

            ResourceDispatchRecord record = new ResourceDispatchRecord();
            record.setRecordId(UUID.randomUUID().toString());
            record.setResourceId(resourceId);
            record.setResourceName(resource.getResourceName());
            record.setIncidentId(incidentId);
            record.setPlanId(planId);
            record.setDispatchType("lock");
            record.setQuantity(quantity);
            record.setFromLocation(resource.getLocation());
            record.setOperatorId(getCurrentUserId());
            record.setOperatorName(getCurrentUsername());
            record.setStatus("completed");
            record.setRemark(remark);

            dispatchRecordRepository.save(record);

            if (incidentId != null && !incidentId.isEmpty()) {
                incidentRepository.findByIncidentId(incidentId).ifPresent(incident -> {
                    incident.setResourceDispatchStatus("executing");
                    incident.setDisposalPlanStatus("accepted");
                    incidentRepository.save(incident);
                });

                List<Plan> plans = planRepository.findByIncidentId(incidentId);
                for (Plan plan : plans) {
                    if ("submitted".equals(plan.getStatus()) || "resubmitted".equals(plan.getStatus())) {
                        plan.setStatus("accepted");
                        plan.setRejectReason(null);
                        planRepository.save(plan);
                    }
                }
            }

            entityManager.flush();
            
            return Map.of(
                    "success", true,
                    "message", "资源锁定成功",
                    "resource", resource,
                    "recordId", record.getRecordId()
            );
        } finally {
            redisLockUtil.unlock(lockKey);
        }
    }

    @Transactional("mysqlTransactionManager")
    @SystemAuditLog(module = "resource", action = "release", actionType = "UPDATE")
    public Map<String, Object> releaseResource(String resourceId, Integer quantity, String remark) {
        String lockKey = "resource:" + resourceId;

        if (!redisLockUtil.lock(lockKey)) {
            throw new RuntimeException("资源正在被其他操作占用，请稍后重试");
        }

        try {
            EmergencyResource resource = resourceRepository.findByResourceId(resourceId)
                    .orElseThrow(() -> new IllegalArgumentException("资源不存在"));

            if (resource.getLockedStock() < quantity) {
                throw new IllegalArgumentException("已锁定库存不足，当前锁定: " + resource.getLockedStock());
            }

            int updated = resourceRepository.releaseResource(resourceId, quantity);
            if (updated == 0) {
                throw new RuntimeException("释放资源失败");
            }

            entityManager.flush();
            entityManager.clear();

            resource = resourceRepository.findByResourceId(resourceId)
                    .orElseThrow(() -> new IllegalArgumentException("资源不存在"));

            ResourceDispatchRecord record = new ResourceDispatchRecord();
            record.setRecordId(UUID.randomUUID().toString());
            record.setResourceId(resourceId);
            record.setResourceName(resource.getResourceName());
            record.setDispatchType("release");
            record.setQuantity(quantity);
            record.setToLocation(resource.getLocation());
            record.setOperatorId(getCurrentUserId());
            record.setOperatorName(getCurrentUsername());
            record.setStatus("completed");
            record.setRemark(remark);

            dispatchRecordRepository.save(record);

            entityManager.flush();

            return Map.of(
                    "success", true,
                    "message", "资源释放成功",
                    "resource", resource,
                    "recordId", record.getRecordId()
            );
        } finally {
            redisLockUtil.unlock(lockKey);
        }
    }

    @Transactional("mysqlTransactionManager")
    @SystemAuditLog(module = "resource", action = "allocate", actionType = "UPDATE")
    public Map<String, Object> allocateResource(String resourceId, Integer quantity, String incidentId, String planId, String remark) {
        String lockKey = "resource:" + resourceId;

        if (!redisLockUtil.lock(lockKey)) {
            throw new RuntimeException("资源正在被其他操作占用，请稍后重试");
        }

        try {
            EmergencyResource resource = resourceRepository.findByResourceId(resourceId)
                    .orElseThrow(() -> new IllegalArgumentException("资源不存在"));

            if (resource.getLockedStock() < quantity) {
                throw new IllegalArgumentException("已锁定库存不足，当前锁定: " + resource.getLockedStock());
            }

            int updated = resourceRepository.allocateResource(resourceId, quantity);
            if (updated == 0) {
                throw new RuntimeException("分配资源失败");
            }

            entityManager.flush();
            entityManager.clear();

            resource = resourceRepository.findByResourceId(resourceId)
                    .orElseThrow(() -> new IllegalArgumentException("资源不存在"));

            ResourceDispatchRecord record = new ResourceDispatchRecord();
            record.setRecordId(UUID.randomUUID().toString());
            record.setResourceId(resourceId);
            record.setResourceName(resource.getResourceName());
            record.setIncidentId(incidentId);
            record.setPlanId(planId);
            record.setDispatchType("allocate");
            record.setQuantity(quantity);
            record.setFromLocation(resource.getLocation());
            record.setOperatorId(getCurrentUserId());
            record.setOperatorName(getCurrentUsername());
            record.setStatus("completed");
            record.setRemark(remark);

            dispatchRecordRepository.save(record);

            entityManager.flush();

            logger.info("资源分配出库成功，resourceId: {}, quantity: {}, incidentId: {}", resourceId, quantity, incidentId);

            return Map.of(
                    "success", true,
                    "message", "资源分配成功",
                    "resource", resource,
                    "recordId", record.getRecordId()
            );
        } finally {
            redisLockUtil.unlock(lockKey);
        }
    }

    @Transactional("mysqlTransactionManager")
    public EmergencyResource createResource(EmergencyResource resource) {
        return resourceRepository.save(resource);
    }

    @Transactional("mysqlTransactionManager")
    public EmergencyResource updateResource(Long id, com.project.controller.ResourceController.UpdateResourceRequest request) {
        EmergencyResource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("资源不存在，id: " + id));

        if (request.getResourceName() != null) {
            resource.setResourceName(request.getResourceName());
        }
        if (request.getResourceType() != null) {
            resource.setResourceType(request.getResourceType());
        }
        if (request.getUnit() != null) {
            resource.setUnit(request.getUnit());
        }
        if (request.getTotalStock() != null) {
            int delta = request.getTotalStock() - (resource.getTotalStock() != null ? resource.getTotalStock() : 0);
            resource.setTotalStock(request.getTotalStock());
            if (request.getAvailableStock() == null) {
                resource.setAvailableStock((resource.getAvailableStock() != null ? resource.getAvailableStock() : 0) + delta);
            }
        }
        if (request.getAvailableStock() != null) {
            resource.setAvailableStock(request.getAvailableStock());
        }
        if (request.getLocation() != null) {
            resource.setLocation(request.getLocation());
        }
        if (request.getDescription() != null) {
            resource.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            resource.setStatus(request.getStatus());
        }

        EmergencyResource saved = resourceRepository.save(resource);
        entityManager.flush();
        entityManager.refresh(saved);
        return saved;
    }

    @Transactional("mysqlTransactionManager")
    public void deleteResource(Long id) {
        if (!resourceRepository.existsById(id)) {
            throw new IllegalArgumentException("资源不存在，id: " + id);
        }
        resourceRepository.deleteById(id);
    }

    public List<ResourceDispatchRecord> getDispatchRecords(String resourceId, String incidentId) {
        if (resourceId != null && !resourceId.isEmpty()) {
            return dispatchRecordRepository.findByResourceId(resourceId);
        } else if (incidentId != null && !incidentId.isEmpty()) {
            return dispatchRecordRepository.findByIncidentId(incidentId);
        }
        return dispatchRecordRepository.findAll();
    }

    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
                return 1L;
            }
        } catch (Exception e) {
            logger.error("获取当前用户ID失败", e);
        }
        return null;
    }

    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                return userDetails.getUsername();
            }
        } catch (Exception e) {
            logger.error("获取当前用户名失败", e);
        }
        return null;
    }
}