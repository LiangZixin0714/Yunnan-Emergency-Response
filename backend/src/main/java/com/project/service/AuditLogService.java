package com.project.service;

import com.project.dto.ai.PageResult;
import com.project.entity.mysql.AuditLog;
import com.project.repository.mysql.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public PageResult<AuditLog> listLogs(Integer page, Integer size, Long userId, String module, String action) {
        List<AuditLog> allLogs = auditLogRepository.findAll();

        if (userId != null) {
            allLogs = allLogs.stream().filter(l -> userId.equals(l.getUserId())).toList();
        }
        if (module != null && !module.isEmpty()) {
            allLogs = allLogs.stream().filter(l -> module.equals(l.getModule())).toList();
        }
        if (action != null && !action.isEmpty()) {
            allLogs = allLogs.stream().filter(l -> action.equals(l.getAction())).toList();
        }

        allLogs = allLogs.stream()
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .toList();

        long total = allLogs.size();
        int start = (page - 1) * size;
        int end = (int) Math.min(start + size, total);

        List<AuditLog> pageList = start < total ? allLogs.subList(start, end) : new ArrayList<>();

        return new PageResult<>(pageList, total, page, size);
    }

    public AuditLog getLogById(Long id) {
        return auditLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("审计日志不存在"));
    }
}