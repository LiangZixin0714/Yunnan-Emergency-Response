package com.project.repository.mysql;

import com.project.entity.mysql.ResourceDispatchRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceDispatchRecordRepository extends JpaRepository<ResourceDispatchRecord, Long> {

    List<ResourceDispatchRecord> findByResourceId(String resourceId);

    List<ResourceDispatchRecord> findByIncidentId(String incidentId);

    List<ResourceDispatchRecord> findByOperatorId(Long operatorId);

    List<ResourceDispatchRecord> findByStatus(String status);

    List<ResourceDispatchRecord> findByDispatchType(String dispatchType);
}