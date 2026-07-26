package com.project.dto.incident;

import jakarta.validation.constraints.NotBlank;

public class IncidentRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;

    @NotBlank(message = "灾情类型不能为空")
    private String incidentType;

    @NotBlank(message = "严重程度不能为空")
    private String severity;

    private String location;

    private Double latitude;

    private Double longitude;

    public IncidentRequest() {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}