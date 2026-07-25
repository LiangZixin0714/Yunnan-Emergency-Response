package com.project.repository.mysql;

import com.project.entity.mysql.ResourceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRequestRepository extends JpaRepository<ResourceRequest, Long> {

    List<ResourceRequest> findByIncidentId(String incidentId);

    List<ResourceRequest> findByIncidentIdOrderByCreatedAtDesc(String incidentId);
}
