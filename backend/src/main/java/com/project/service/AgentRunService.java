package com.project.service;

import com.project.dto.ai.AgentRunLogRequest;
import com.project.dto.ai.CitationItem;
import com.project.dto.ai.PageResult;
import com.project.entity.mysql.AgentRun;
import com.project.entity.mysql.Citation;
import com.project.repository.mysql.AgentRunRepository;
import com.project.repository.mysql.CitationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AgentRunService {

    private static final Logger logger = LoggerFactory.getLogger(AgentRunService.class);

    private final AgentRunRepository agentRunRepository;
    private final CitationRepository citationRepository;

    public AgentRunService(AgentRunRepository agentRunRepository, CitationRepository citationRepository) {
        this.agentRunRepository = agentRunRepository;
        this.citationRepository = citationRepository;
    }

    @Transactional("mysqlTransactionManager")
    public void logRun(AgentRunLogRequest request) {
        if (agentRunRepository.findByRunId(request.getRunId()).isPresent()) {
            throw new IllegalStateException("执行记录已存在");
        }

        String status = request.getStatus();
        if (!"success".equals(status) && !"failed".equals(status) && !"timeout".equals(status)) {
            throw new IllegalArgumentException("status必须为success/failed/timeout");
        }

        AgentRun agentRun = new AgentRun();
        agentRun.setRunId(request.getRunId());
        agentRun.setIncidentId(request.getIncidentId());
        agentRun.setAgentName(request.getAgentName());
        agentRun.setInputParams(request.getInputParams());
        agentRun.setOutputResult(request.getOutputResult());
        agentRun.setStatus(request.getStatus());
        agentRun.setErrorMessage(request.getErrorMessage());
        agentRun.setStartTime(request.getStartTime());
        agentRun.setEndTime(request.getEndTime());

        agentRunRepository.save(agentRun);

        if (request.getCitations() != null && !request.getCitations().isEmpty()) {
            for (CitationItem item : request.getCitations()) {
                Citation citation = new Citation();
                citation.setCitationId(item.getCitationId() != null ? item.getCitationId() : UUID.randomUUID().toString());
                citation.setIncidentId(request.getIncidentId());
                citation.setRunId(request.getRunId());
                citation.setSourceText(item.getSourceText());
                citation.setSourceUrl(item.getSourceUrl());
                citation.setRelevanceScore(item.getRelevanceScore());
                citationRepository.save(citation);
            }
        }

        logger.info("Agent执行记录写入成功: runId={}, agentName={}, status={}", request.getRunId(), request.getAgentName(), request.getStatus());
    }

    public PageResult<AgentRun> listRuns(Integer page, Integer size, String incidentId, String agentName, String status) {
        List<AgentRun> allRuns = agentRunRepository.findAll();

        if (incidentId != null && !incidentId.isEmpty()) {
            allRuns = allRuns.stream().filter(r -> incidentId.equals(r.getIncidentId())).toList();
        }
        if (agentName != null && !agentName.isEmpty()) {
            allRuns = allRuns.stream().filter(r -> agentName.equals(r.getAgentName())).toList();
        }
        if (status != null && !status.isEmpty()) {
            allRuns = allRuns.stream().filter(r -> status.equals(r.getStatus())).toList();
        }

        long total = allRuns.size();
        int start = (page - 1) * size;
        int end = (int) Math.min(start + size, total);

        List<AgentRun> pageList = start < total ? allRuns.subList(start, end) : new ArrayList<>();

        return new PageResult<>(pageList, total, page, size);
    }

    public AgentRun getRunById(Long id) {
        return agentRunRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("执行记录不存在"));
    }

    public List<Citation> getCitationsByRunId(Long id) {
        AgentRun agentRun = agentRunRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("执行记录不存在"));
        return citationRepository.findByRunId(agentRun.getRunId());
    }
}