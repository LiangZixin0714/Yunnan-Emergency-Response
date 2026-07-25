package com.project.controller;

import com.project.common.Result;
import com.project.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ResponseEntity<Result<Map<String, Object>>> getOverview() {
        Map<String, Object> overview = dashboardService.getOverview();
        return ResponseEntity.ok(Result.success(overview));
    }

    @GetMapping("/trend")
    public ResponseEntity<Result<Map<String, Object>>> getTrend() {
        Map<String, Object> trend = dashboardService.getTrend();
        return ResponseEntity.ok(Result.success(trend));
    }

    @GetMapping("/distribution")
    public ResponseEntity<Result<Map<String, Object>>> getDistribution() {
        Map<String, Object> distribution = dashboardService.getDistribution();
        return ResponseEntity.ok(Result.success(distribution));
    }

    @GetMapping("/screen")
    public ResponseEntity<Result<Map<String, Object>>> getScreenData() {
        Map<String, Object> screen = dashboardService.getScreenData();
        return ResponseEntity.ok(Result.success(screen));
    }
}
