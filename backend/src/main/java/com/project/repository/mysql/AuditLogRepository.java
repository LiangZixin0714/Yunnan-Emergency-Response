package com.project.repository.mysql;

import com.project.entity.mysql.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserId(Long userId);

    List<AuditLog> findByAction(String action);

    List<AuditLog> findByTargetType(String targetType);

    List<AuditLog> findByModule(String module);

    List<AuditLog> findByModuleAndAction(String module, String action);
}