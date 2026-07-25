package com.project.controller;

import com.project.common.Result;
import com.project.dto.ai.AgentRunLogRequest;
import com.project.dto.ai.PageResult;
import com.project.entity.mysql.AgentRun;
import com.project.entity.mysql.Citation;
import com.project.service.AgentRunService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
public class AgentRunController {

    private final AgentRunService agentRunService;

    public AgentRunController(AgentRunService agentRunService) {
        this.agentRunService = agentRunService;
    }

    @PostMapping("/log")
    public ResponseEntity<Result<Void>> logRun(@Valid @RequestBody AgentRunLogRequest request) {
        try {
            agentRunService.logRun(request);
            return ResponseEntity.ok(Result.success("执行记录写入成功", null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Result.error(409, e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Result.badRequest(e.getMessage()));
        }
    }

    @GetMapping("/runs")
    public ResponseEntity<Result<PageResult<AgentRun>>> listRuns(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String incidentId,
            @RequestParam(required = false) String agentName,
            @RequestParam(required = false) String status) {
        PageResult<AgentRun> result = agentRunService.listRuns(page, size, incidentId, agentName, status);
        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/runs/{id}")
    public ResponseEntity<Result<AgentRun>> getRun(@PathVariable Long id) {
        try {
            AgentRun agentRun = agentRunService.getRunById(id);
            return ResponseEntity.ok(Result.success(agentRun));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Result.notFound(e.getMessage()));
        }
    }

    @GetMapping("/runs/{id}/citations")
    public ResponseEntity<Result<List<Citation>>> getCitations(@PathVariable Long id) {
        try {
            List<Citation> citations = agentRunService.getCitationsByRunId(id);
            return ResponseEntity.ok(Result.success(citations));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Result.notFound(e.getMessage()));
        }
    }
}