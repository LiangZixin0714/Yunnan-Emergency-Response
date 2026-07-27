package com.project.entity.mysql;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", unique = true, nullable = false, length = 50)
    private String incidentId;

    @Column(name = "incident_name", nullable = false, length = 200)
    private String incidentName;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "disaster_type", nullable = false, length = 50)
    private String disasterType;

    @Column(name = "incident_level", length = 10)
    private String incidentLevel;

    @Column(name = "occur_time")
    private LocalDateTime occurTime;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "disposal_plan_status", length = 20)
    private String disposalPlanStatus;

    @Column(name = "resource_dispatch_status", length = 20)
    private String resourceDispatchStatus;

    @Column(name = "reject_type", length = 20)
    private String rejectType;

    @Column(name = "image_urls", columnDefinition = "TEXT")
    private String imageUrls;

    @Column(name = "reporter_id")
    private Long reporterId;

    @Column(name = "death_count")
    private Integer deathCount;

    @Column(name = "property_loss")
    private Double propertyLoss;

    @Column(name = "report_time")
    private LocalDateTime reportTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Incident() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        reportTime = LocalDateTime.now();
        if (status == null) {
            status = "processing";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getIncidentName() {
        return incidentName;
    }

    public void setIncidentName(String incidentName) {
        this.incidentName = incidentName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDisasterType() {
        return disasterType;
    }

    public void setDisasterType(String disasterType) {
        this.disasterType = disasterType;
    }

    public String getIncidentLevel() {
        return incidentLevel;
    }

    public void setIncidentLevel(String incidentLevel) {
        this.incidentLevel = incidentLevel;
    }

    public LocalDateTime getOccurTime() {
        return occurTime;
    }

    public void setOccurTime(LocalDateTime occurTime) {
        this.occurTime = occurTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDisposalPlanStatus() {
        return disposalPlanStatus;
    }

    public void setDisposalPlanStatus(String disposalPlanStatus) {
        this.disposalPlanStatus = disposalPlanStatus;
    }

    public String getResourceDispatchStatus() {
        return resourceDispatchStatus;
    }

    public void setResourceDispatchStatus(String resourceDispatchStatus) {
        this.resourceDispatchStatus = resourceDispatchStatus;
    }

    public String getRejectType() {
        return rejectType;
    }

    public void setRejectType(String rejectType) {
        this.rejectType = rejectType;
    }

    public String getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(String imageUrls) {
        this.imageUrls = imageUrls;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }

    public Integer getDeathCount() {
        return deathCount;
    }

    public void setDeathCount(Integer deathCount) {
        this.deathCount = deathCount;
    }

    public Double getPropertyLoss() {
        return propertyLoss;
    }

    public void setPropertyLoss(Double propertyLoss) {
        this.propertyLoss = propertyLoss;
    }

    public LocalDateTime getReportTime() {
        return reportTime;
    }

    public void setReportTime(LocalDateTime reportTime) {
        this.reportTime = reportTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}