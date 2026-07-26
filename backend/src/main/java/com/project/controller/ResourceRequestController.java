package com.project.controller;

import com.project.common.Result;
import com.project.entity.mysql.ResourceRequest;
import com.project.service.ResourceRequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resource-request")
@Validated
public class ResourceRequestController {

    private final ResourceRequestService resourceRequestService;

    public ResourceRequestController(ResourceRequestService resourceRequestService) {
        this.resourceRequestService = resourceRequestService;
    }

    @GetMapping("/list")
    public ResponseEntity<Result<List<ResourceRequest>>> list(@RequestParam String incidentId) {
        List<ResourceRequest> requests = resourceRequestService.listByIncidentId(incidentId);
        return ResponseEntity.ok(Result.success(requests));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Result<List<ResourceRequest>>> listPending() {
        List<ResourceRequest> requests = resourceRequestService.listPending();
        return ResponseEntity.ok(Result.success(requests));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE_MANAGER')")
    public ResponseEntity<Result<List<ResourceRequest>>> submit(@Valid @RequestBody SubmitRequest request) {
        List<ResourceRequest> requests = resourceRequestService.submitBatch(
                request.getIncidentId(),
                request.getItems(),
                request.getRequesterId()
        );
        return ResponseEntity.ok(Result.success(requests));
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Result<ResourceRequest>> approve(@Valid @RequestBody ActionRequest request) {
        ResourceRequest req = resourceRequestService.approve(request.getId());
        return ResponseEntity.ok(Result.success(req));
    }

    @PostMapping("/reject")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Result<ResourceRequest>> reject(@Valid @RequestBody RejectRequest request) {
        ResourceRequest req = resourceRequestService.reject(request.getId(), request.getReason());
        return ResponseEntity.ok(Result.success(req));
    }

    public static class SubmitRequest {
        @NotBlank(message = "incidentId不能为空")
        private String incidentId;

        @NotEmpty(message = "资源申请项不能为空")
        private List<ResourceRequestService.ResourceRequestItem> items;

        private Long requesterId;

        public String getIncidentId() { return incidentId; }
        public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
        public List<ResourceRequestService.ResourceRequestItem> getItems() { return items; }
        public void setItems(List<ResourceRequestService.ResourceRequestItem> items) { this.items = items; }
        public Long getRequesterId() { return requesterId; }
        public void setRequesterId(Long requesterId) { this.requesterId = requesterId; }
    }

    public static class ActionRequest {
        @NotNull(message = "id不能为空")
        private Long id;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

    public static class RejectRequest {
        @NotNull(message = "id不能为空")
        private Long id;

        private String reason;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
