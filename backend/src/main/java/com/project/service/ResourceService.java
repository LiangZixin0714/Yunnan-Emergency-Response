package com.project.service;

import com.project.annotation.SystemAuditLog;
import com.project.entity.mysql.EmergencyResource;
import com.project.entity.mysql.ResourceDispatchRecord;
import com.project.repository.mysql.EmergencyResourceRepository;
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
    private final RedisLockUtil redisLockUtil;

    @PersistenceContext(unitName = "mysql")
    private EntityManager entityManager;

    public ResourceService(EmergencyResourceRepository resourceRepository,
                          ResourceDispatchRecordRepository dispatchRecordRepository,
                          RedisLockUtil redisLockUtil) {
        this.resourceRepository = resourceRepository;
        this.dispatchRecordRepository = dispatchRecordRepository;
        this.redisLockUtil = redisLockUtil;
    }

    public List<EmergencyResource> getAvailableResources() {
        return resourceRepository.findByAvailableStockGreaterThan(0);
    }

    public List<EmergencyResource> getAllResources() {
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
            EmergencyResource resource = resourceRepository.findByResourceId(resourceId)
                    .orElseThrow(() -> new IllegalArgumentException("资源不存在"));

            if (resource.getAvailableStock() < quantity) {
                throw new IllegalArgumentException("可用库存不足，当前可用: " + resource.getAvailableStock());
            }

            int updated = resourceRepository.lockResource(resourceId, quantity);
            if (updated == 0) {
                throw new RuntimeException("锁定资源失败");
            }

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

            entityManager.flush();
            entityManager.refresh(resource);
            
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
            entityManager.refresh(resource);

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