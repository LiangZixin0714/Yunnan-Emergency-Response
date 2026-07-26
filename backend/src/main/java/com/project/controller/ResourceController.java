package com.project.controller;

import com.project.common.Result;
import com.project.entity.mysql.EmergencyResource;
import com.project.entity.mysql.ResourceDispatchRecord;
import com.project.service.ResourceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/resource")
@Validated
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("/available")
    public ResponseEntity<Result<List<EmergencyResource>>> getAvailableResources() {
        List<EmergencyResource> resources = resourceService.getAvailableResources();
        return ResponseEntity.ok(Result.success(resources));
    }

    @GetMapping("/list")
    public ResponseEntity<Result<List<EmergencyResource>>> getAllResources() {
        List<EmergencyResource> resources = resourceService.getAllResources();
        return ResponseEntity.ok(Result.success(resources));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Result<EmergencyResource>> createResource(
            @Valid @RequestBody CreateResourceRequest request) {
        EmergencyResource resource = new EmergencyResource();
        resource.setResourceId("res-" + UUID.randomUUID().toString().substring(0, 8));
        resource.setResourceName(request.getResourceName());
        resource.setResourceType(request.getResourceType());
        resource.setUnit(request.getUnit());
        resource.setTotalStock(request.getTotalStock() != null ? request.getTotalStock() : 0);
        resource.setAvailableStock(request.getAvailableStock() != null ? request.getAvailableStock() : request.getTotalStock());
        resource.setLockedStock(0);
        resource.setLocation(request.getLocation());
        resource.setDescription(request.getDescription());
        resource.setStatus("available");
        EmergencyResource saved = resourceService.createResource(resource);
        return ResponseEntity.ok(Result.success(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Result<EmergencyResource>> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody UpdateResourceRequest request) {
        EmergencyResource updated = resourceService.updateResource(id, request);
        return ResponseEntity.ok(Result.success(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Result<Map<String, Object>>> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.ok(Result.success(Map.of("deleted", true)));
    }

    @PostMapping("/lock")
    @PreAuthorize("hasAnyRole('RESOURCE_MANAGER')")
    public ResponseEntity<Result<Map<String, Object>>> lockResource(
            @Valid @RequestBody LockResourceRequest request) {
        Map<String, Object> result = resourceService.lockResource(
                request.getResourceId(),
                request.getQuantity(),
                request.getIncidentId(),
                request.getPlanId(),
                request.getRemark()
        );
        return ResponseEntity.ok(Result.success(result));
    }

    @PostMapping("/release")
    @PreAuthorize("hasAnyRole('RESOURCE_MANAGER')")
    public ResponseEntity<Result<Map<String, Object>>> releaseResource(
            @Valid @RequestBody ReleaseResourceRequest request) {
        Map<String, Object> result = resourceService.releaseResource(
                request.getResourceId(),
                request.getQuantity(),
                request.getRemark()
        );
        return ResponseEntity.ok(Result.success(result));
    }

    @PostMapping("/allocate")
    @PreAuthorize("hasAnyRole('RESOURCE_MANAGER')")
    public ResponseEntity<Result<Map<String, Object>>> allocateResource(
            @Valid @RequestBody AllocateResourceRequest request) {
        Map<String, Object> result = resourceService.allocateResource(
                request.getResourceId(),
                request.getQuantity(),
                request.getIncidentId(),
                request.getPlanId(),
                request.getRemark()
        );
        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/dispatch-records")
    public ResponseEntity<Result<List<ResourceDispatchRecord>>> getDispatchRecords(
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String incidentId) {
        List<ResourceDispatchRecord> records = resourceService.getDispatchRecords(resourceId, incidentId);
        return ResponseEntity.ok(Result.success(records));
    }

    public static class CreateResourceRequest {
        @NotBlank(message = "资源名称不能为空")
        private String resourceName;

        private String resourceType;

        private String unit;

        private Integer totalStock;

        private Integer availableStock;

        private String location;

        private String description;

        public String getResourceName() {
            return resourceName;
        }

        public void setResourceName(String resourceName) {
            this.resourceName = resourceName;
        }

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public Integer getTotalStock() {
            return totalStock;
        }

        public void setTotalStock(Integer totalStock) {
            this.totalStock = totalStock;
        }

        public Integer getAvailableStock() {
            return availableStock;
        }

        public void setAvailableStock(Integer availableStock) {
            this.availableStock = availableStock;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class UpdateResourceRequest {
        private String resourceName;
        private String resourceType;
        private String unit;
        private Integer totalStock;
        private Integer availableStock;
        private String location;
        private String description;
        private String status;

        public String getResourceName() {
            return resourceName;
        }

        public void setResourceName(String resourceName) {
            this.resourceName = resourceName;
        }

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public Integer getTotalStock() {
            return totalStock;
        }

        public void setTotalStock(Integer totalStock) {
            this.totalStock = totalStock;
        }

        public Integer getAvailableStock() {
            return availableStock;
        }

        public void setAvailableStock(Integer availableStock) {
            this.availableStock = availableStock;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class LockResourceRequest {
        @NotBlank(message = "resourceId不能为空")
        private String resourceId;

        @Positive(message = "数量必须大于0")
        private Integer quantity;

        private String incidentId;

        private String planId;

        private String remark;

        public String getResourceId() {
            return resourceId;
        }

        public void setResourceId(String resourceId) {
            this.resourceId = resourceId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public String getIncidentId() {
            return incidentId;
        }

        public void setIncidentId(String incidentId) {
            this.incidentId = incidentId;
        }

        public String getPlanId() {
            return planId;
        }

        public void setPlanId(String planId) {
            this.planId = planId;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    public static class AllocateResourceRequest {
        @NotBlank(message = "resourceId不能为空")
        private String resourceId;

        @Positive(message = "数量必须大于0")
        private Integer quantity;

        private String incidentId;

        private String planId;

        private String remark;

        public String getResourceId() {
            return resourceId;
        }

        public void setResourceId(String resourceId) {
            this.resourceId = resourceId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public String getIncidentId() {
            return incidentId;
        }

        public void setIncidentId(String incidentId) {
            this.incidentId = incidentId;
        }

        public String getPlanId() {
            return planId;
        }

        public void setPlanId(String planId) {
            this.planId = planId;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    public static class ReleaseResourceRequest {
        @NotBlank(message = "resourceId不能为空")
        private String resourceId;

        @Positive(message = "数量必须大于0")
        private Integer quantity;

        private String remark;

        public String getResourceId() {
            return resourceId;
        }

        public void setResourceId(String resourceId) {
            this.resourceId = resourceId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}