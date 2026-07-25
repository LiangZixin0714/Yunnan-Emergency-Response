package com.project.controller;

import com.project.common.Result;
import com.project.dto.incident.IncidentReportRequest;
import com.project.dto.incident.IncidentReportResponse;
import com.project.dto.incident.IncidentRequest;
import com.project.dto.incident.IncidentResponse;
import com.project.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/incident")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping(value = "/report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Result<IncidentReportResponse>> reportIncident(
            @Valid IncidentReportRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        Long reporterId = 1L;
        IncidentReportResponse response = incidentService.reportIncident(request, reporterId);
        return ResponseEntity.ok(Result.success("success", response));
    }

    @GetMapping("/detail")
    public ResponseEntity<Result<com.project.entity.mysql.Incident>> getIncident(
            @RequestParam String incidentId) {
        com.project.entity.mysql.Incident incident = incidentService.getIncidentById(incidentId);
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
        String username = authentication.getName();
        Long reporterId = 1L;
        IncidentResponse response = incidentService.submitIncident(request, reporterId);
        return ResponseEntity.ok(Result.success("success", response));
    }

    @PostMapping("/update-status")
    public ResponseEntity<Result<com.project.entity.mysql.Incident>> updateStatus(
            @RequestParam String incidentId,
            @RequestParam String status) {
        com.project.entity.mysql.Incident incident = incidentService.updateStatus(incidentId, status);
        return ResponseEntity.ok(Result.success(incident));
    }
}