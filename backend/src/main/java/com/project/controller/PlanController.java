package com.project.controller;

import com.project.common.Result;
import com.project.entity.mysql.Plan;
import com.project.service.PlanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plan")
@Validated
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping("/generate")
    public ResponseEntity<Result<Map<String, String>>> generatePlan(
            @Valid @RequestBody GeneratePlanRequest request) {
        Map<String, String> result = planService.generatePlan(request.getIncidentId());
        return ResponseEntity.ok(Result.success("success", result));
    }

    @GetMapping("/detail")
    public ResponseEntity<Result<Plan>> getPlanDetail(
            @RequestParam @NotBlank(message = "planId不能为空") String planId) {
        Plan plan = planService.getPlanById(planId);
        return ResponseEntity.ok(Result.success(plan));
    }

    @GetMapping("/list")
    public ResponseEntity<Result<List<Plan>>> getPlanList(
            @RequestParam @NotBlank(message = "incidentId不能为空") String incidentId) {
        List<Plan> plans = planService.getPlansByIncidentId(incidentId);
        return ResponseEntity.ok(Result.success(plans));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPlan(
            @RequestParam @NotBlank(message = "incidentId不能为空") String incidentId) {
        return planService.streamPlan(incidentId);
    }

    @PostMapping("/review")
    public ResponseEntity<Result<Plan>> reviewPlan(
            @Valid @RequestBody ReviewPlanRequest request) {
        Plan plan = planService.reviewPlan(request.getPlanId(), request.getAction(), 
                request.getModifyContent(), request.getRemark());
        return ResponseEntity.ok(Result.success(plan));
    }

    public static class GeneratePlanRequest {
        @NotBlank(message = "incidentId不能为空")
        private String incidentId;

        public String getIncidentId() {
            return incidentId;
        }

        public void setIncidentId(String incidentId) {
            this.incidentId = incidentId;
        }
    }

    public static class ReviewPlanRequest {
        @NotBlank(message = "planId不能为空")
        private String planId;

        @NotBlank(message = "action不能为空")
        private String action;

        private String modifyContent;

        private String remark;

        public String getPlanId() {
            return planId;
        }

        public void setPlanId(String planId) {
            this.planId = planId;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getModifyContent() {
            return modifyContent;
        }

        public void setModifyContent(String modifyContent) {
            this.modifyContent = modifyContent;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}