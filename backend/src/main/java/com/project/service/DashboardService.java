package com.project.service;

import com.project.entity.mysql.Incident;
import com.project.entity.mysql.EmergencyResource;
import com.project.repository.mysql.IncidentRepository;
import com.project.repository.mysql.EmergencyResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    private final IncidentRepository incidentRepository;
    private final EmergencyResourceRepository resourceRepository;

    public DashboardService(IncidentRepository incidentRepository,
                            EmergencyResourceRepository resourceRepository) {
        this.incidentRepository = incidentRepository;
        this.resourceRepository = resourceRepository;
    }

    public Map<String, Object> getOverview() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        long todayCount = incidentRepository.countByCreatedAtBetween(todayStart, todayEnd);
        long activeCount = incidentRepository.countByStatus("processing");
        long completedCount = incidentRepository.countByStatus("completed");

        Map<String, Object> overview = new HashMap<>();
        overview.put("todayCount", todayCount);
        overview.put("activeCount", activeCount);
        overview.put("completedCount", completedCount);
        return overview;
    }

    public Map<String, Object> getTrend() {
        LocalDate startDate = LocalDate.now().minusDays(6);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        List<Object[]> results = incidentRepository.countByDateGroup(startDateTime);

        List<String> dates = new ArrayList<>();
        List<Long> counts = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            dates.add(startDate.plusDays(i).toString());
            counts.add(0L);
        }

        for (Object[] row : results) {
            String date = (String) row[0];
            Long count = (Long) row[1];
            int index = dates.indexOf(date);
            if (index >= 0) {
                counts.set(index, count);
            }
        }

        Map<String, Object> trend = new HashMap<>();
        trend.put("dates", dates);
        trend.put("counts", counts);
        return trend;
    }

    public Map<String, Object> getDistribution() {
        List<Object[]> results = incidentRepository.countByDisasterTypeGroup();

        List<String> types = new ArrayList<>();
        List<Long> counts = new ArrayList<>();

        for (Object[] row : results) {
            String type = (String) row[0];
            Long count = (Long) row[1];
            types.add(type != null ? type : "其他");
            counts.add(count);
        }

        Map<String, Object> distribution = new HashMap<>();
        distribution.put("types", types);
        distribution.put("counts", counts);
        return distribution;
    }

    public Map<String, Object> getScreenData() {
        Map<String, Object> screen = new HashMap<>();

        Map<String, Object> statistics = getOverview();
        screen.put("statistics", statistics);

        List<Incident> incidents = incidentRepository.findRecentIncidents(PageRequest.of(0, 10));
        screen.put("incidents", incidents);

        List<EmergencyResource> resources = resourceRepository.findAll();
        screen.put("resources", resources);

        List<Incident> mapIncidents = incidentRepository.findWithCoordinates();
        screen.put("mapIncidents", mapIncidents);

        return screen;
    }
}
