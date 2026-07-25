package com.project.repository.mysql;

import com.project.entity.mysql.Incident;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findByIncidentId(String incidentId);

    List<Incident> findByStatus(String status);

    List<Incident> findByReporterId(Long reporterId);

    List<Incident> findByDisasterType(String disasterType);

    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    long countByStatus(String status);

    @Query("SELECT i.disasterType, COUNT(i) FROM Incident i GROUP BY i.disasterType")
    List<Object[]> countByDisasterTypeGroup();

    @Query("SELECT FUNCTION('DATE_FORMAT', i.createdAt, '%Y-%m-%d') as date, COUNT(i) as count FROM Incident i WHERE i.createdAt >= :startDate GROUP BY FUNCTION('DATE_FORMAT', i.createdAt, '%Y-%m-%d') ORDER BY date")
    List<Object[]> countByDateGroup(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT i FROM Incident i WHERE i.status = 'processing' ORDER BY i.createdAt DESC")
    List<Incident> findActiveIncidents();

    @Query("SELECT i FROM Incident i ORDER BY i.createdAt DESC")
    List<Incident> findRecentIncidents(Pageable pageable);
}