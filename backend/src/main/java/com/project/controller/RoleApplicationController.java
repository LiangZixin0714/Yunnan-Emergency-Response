package com.project.controller;

import com.project.common.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/role-application")
public class RoleApplicationController {

    @GetMapping("/list")
    public ResponseEntity<Result<List<Map<String, Object>>>> getRoleApplicationList(
            @RequestParam(required = false) Integer userId) {
        return ResponseEntity.ok(Result.success(new ArrayList<>()));
    }

    @PostMapping("/submit")
    public ResponseEntity<Result<Map<String, Object>>> submitRoleApplication(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Result.success(Map.of(
                "id", 0,
                "userId", request.get("userId"),
                "roleName", request.get("roleName"),
                "status", "pending",
                "applyTime", java.time.LocalDateTime.now().toString(),
                "reason", request.get("reason")
        )));
    }

    @PostMapping("/review")
    public ResponseEntity<Result<Map<String, Object>>> reviewRoleApplication(@RequestBody Map<String, Object> request) {
        boolean approved = Boolean.TRUE.equals(request.get("approved"));
        return ResponseEntity.ok(Result.success(Map.of(
                "id", request.get("id"),
                "status", approved ? "approved" : "rejected",
                "reviewReason", request.get("reason"),
                "reviewTime", java.time.LocalDateTime.now().toString()
        )));
    }

    @PostMapping("/receive")
    public ResponseEntity<Result<Map<String, Object>>> receiveRoleApplication(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Result.success(Map.of(
                "id", request.get("id"),
                "status", "received"
        )));
    }
}