package com.project.controller;

import com.project.common.Result;
import com.project.entity.mysql.Plan;
import com.project.service.DisposalPlanService;
import com.project.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/disposal-plan")
@Validated
public class DisposalPlanController {

    private final DisposalPlanService disposalPlanService;
    private final ExportService exportService;

    public DisposalPlanController(DisposalPlanService disposalPlanService, ExportService exportService) {
        this.disposalPlanService = disposalPlanService;
        this.exportService = exportService;
    }

    @GetMapping("/list")
    public ResponseEntity<Result<List<Plan>>> list(@RequestParam String incidentId) {
        List<Plan> plans = disposalPlanService.listByIncidentId(incidentId);
        return ResponseEntity.ok(Result.success(plans));
    }

    @PostMapping("/save-draft")
    public ResponseEntity<Result<Plan>> saveDraft(@Valid @RequestBody SaveDraftRequest request) {
        Plan plan = disposalPlanService.saveDraft(
                request.getId(),
                request.getPlanContent(),
                request.getIncidentId()
        );
        return ResponseEntity.ok(Result.success(plan));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Result<Plan>> submit(@Valid @RequestBody SubmitRequest request) {
        Plan plan = disposalPlanService.submit(
                request.getId(),
                request.getPlanContent(),
                request.getIncidentId(),
                request.getSubmittedBy()
        );
        return ResponseEntity.ok(Result.success(plan));
    }

    @PostMapping("/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE_MANAGER')")
    public ResponseEntity<Result<Plan>> reject(@Valid @RequestBody RejectRequest request) {
        Plan plan = disposalPlanService.reject(
                request.getId(),
                request.getRejectReason(),
                request.getIncidentId()
        );
        return ResponseEntity.ok(Result.success(plan));
    }

    @GetMapping("/export/pdf")
    public void exportPdf(@RequestParam String planId, HttpServletResponse response) throws Exception {
        byte[] pdfContent = exportService.exportToPdf(planId);
        
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        String filename = URLEncoder.encode("处置方案_" + planId + ".pdf", StandardCharsets.UTF_8);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.setContentLength(pdfContent.length);
        response.getOutputStream().write(pdfContent);
        response.getOutputStream().flush();
    }

    @GetMapping("/export/word")
    public void exportWord(@RequestParam String planId, HttpServletResponse response) throws Exception {
        byte[] wordContent = exportService.exportToWord(planId);
        
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        String filename = URLEncoder.encode("处置方案_" + planId + ".docx", StandardCharsets.UTF_8);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.setContentLength(wordContent.length);
        response.getOutputStream().write(wordContent);
        response.getOutputStream().flush();
    }

    public static class SaveDraftRequest {
        private Long id;
        @NotBlank(message = "planContent不能为空")
        private String planContent;
        private String incidentId;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getPlanContent() { return planContent; }
        public void setPlanContent(String planContent) { this.planContent = planContent; }
        public String getIncidentId() { return incidentId; }
        public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
    }

    public static class SubmitRequest {
        private Long id;
        private String planContent;
        private String incidentId;
        private Long submittedBy;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getPlanContent() { return planContent; }
        public void setPlanContent(String planContent) { this.planContent = planContent; }
        public String getIncidentId() { return incidentId; }
        public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
        public Long getSubmittedBy() { return submittedBy; }
        public void setSubmittedBy(Long submittedBy) { this.submittedBy = submittedBy; }
    }

    public static class RejectRequest {
        @NotNull(message = "id不能为空")
        private Long id;
        private String rejectReason;
        private String incidentId;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getRejectReason() { return rejectReason; }
        public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
        public String getIncidentId() { return incidentId; }
        public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
    }
}
