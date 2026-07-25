package com.project.controller;

import com.project.common.Result;
import com.project.dto.ai.PageResult;
import com.project.entity.mysql.AuditLog;
import com.project.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<PageResult<AuditLog>>> listLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action) {
        PageResult<AuditLog> result = auditLogService.listLogs(page, size, userId, module, action);
        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/logs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<AuditLog>> getLog(@PathVariable Long id) {
        try {
            AuditLog auditLog = auditLogService.getLogById(id);
            return ResponseEntity.ok(Result.success(auditLog));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Result.notFound(e.getMessage()));
        }
    }
}