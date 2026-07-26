package com.project.entity.mysql;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "citations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Citation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "citation_id", unique = true, nullable = false, length = 64)
    private String citationId;

    @Column(name = "incident_id", length = 64)
    private String incidentId;

    @Column(name = "run_id", length = 64)
    private String runId;

    @Column(name = "source_text", columnDefinition = "TEXT")
    private String sourceText;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "relevance_score", precision = 5, scale = 4)
    private BigDecimal relevanceScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}