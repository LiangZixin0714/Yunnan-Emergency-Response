package com.project.repository.mysql;

import com.project.entity.mysql.Citation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CitationRepository extends JpaRepository<Citation, Long> {

    Optional<Citation> findByCitationId(String citationId);

    List<Citation> findByIncidentId(String incidentId);

    List<Citation> findByRunId(String runId);
}