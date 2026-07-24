package com.project.controller;

import com.project.common.Result;
import com.project.entity.mysql.EmergencyResource;
import com.project.entity.mysql.ResourceDispatchRecord;
import com.project.service.ResourceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @PostMapping("/lock")
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
    public ResponseEntity<Result<Map<String, Object>>> releaseResource(
            @Valid @RequestBody ReleaseResourceRequest request) {
        Map<String, Object> result = resourceService.releaseResource(
                request.getResourceId(),
                request.getQuantity(),
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