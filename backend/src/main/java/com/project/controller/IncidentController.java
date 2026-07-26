package com.project.controller;

import com.project.common.Result;
import com.project.dto.incident.IncidentReportRequest;
import com.project.dto.incident.IncidentReportResponse;
import com.project.dto.incident.IncidentRequest;
import com.project.dto.incident.IncidentResponse;
import com.project.entity.mysql.Incident;
import com.project.entity.mysql.User;
import com.project.repository.mysql.UserRepository;
import com.project.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/incident")
public class IncidentController {

    private final IncidentService incidentService;
    private final UserRepository userRepository;

    public IncidentController(IncidentService incidentService, UserRepository userRepository) {
        this.incidentService = incidentService;
        this.userRepository = userRepository;
    }

    private Long getUserId(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + username));
    }

    @PostMapping(value = "/report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Result<IncidentReportResponse>> reportIncident(
            @Valid IncidentReportRequest request,
            Authentication authentication) {
        Long reporterId = getUserId(authentication);
        IncidentReportResponse response = incidentService.reportIncident(request, reporterId);
        return ResponseEntity.ok(Result.success("success", response));
    }

    @GetMapping("/detail")
    public ResponseEntity<Result<Incident>> getIncident(
            @RequestParam String incidentId) {
        Incident incident = incidentService.getIncidentById(incidentId);
        return ResponseEntity.ok(Result.success(incident));
    }

    @GetMapping("/list")
    public ResponseEntity<Result<Object>> listIncidents(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String disasterType,
            @RequestParam(required = false) String incidentLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(Result.success(incidentService.listIncidents(page, size, disasterType, incidentLevel, status, keyword)));
    }

    @PostMapping("/submit")
    public ResponseEntity<Result<IncidentResponse>> submitIncident(
            @Valid @RequestBody IncidentRequest request,
            Authentication authentication) {
        Long reporterId = getUserId(authentication);
        IncidentResponse response = incidentService.submitIncident(request, reporterId);
        return ResponseEntity.ok(Result.success("success", response));
    }

    @PostMapping("/update-status")
    public ResponseEntity<Result<Incident>> updateStatus(
            @RequestParam String incidentId,
            @RequestParam String status) {
        Incident incident = incidentService.updateStatus(incidentId, status);
        return ResponseEntity.ok(Result.success(incident));
    }

    @PostMapping("/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE_MANAGER')")
    public ResponseEntity<Result<Incident>> completeIncident(
            @RequestParam String incidentId) {
        Incident incident = incidentService.completeIncident(incidentId);
        return ResponseEntity.ok(Result.success(incident));
    }

    @PostMapping("/backfill-coordinates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<Map<String, Object>>> backfillCoordinates() {
        Map<String, Object> result = incidentService.backfillCoordinates();
        return ResponseEntity.ok(Result.success(result));
    }
}