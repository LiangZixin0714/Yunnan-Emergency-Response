package com.project.entity.mysql;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_runs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", unique = true, nullable = false, length = 64)
    private String runId;

    @Column(name = "incident_id", length = 64)
    private String incidentId;

    @Column(name = "agent_name", length = 100)
    private String agentName;

    @Column(name = "input_params", columnDefinition = "TEXT")
    private String inputParams;

    @Column(name = "output_result", columnDefinition = "TEXT")
    private String outputResult;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "start_time")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "end_time")
    private LocalDateTime endTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
    }
}