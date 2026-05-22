package com.diasmart.springapi.alerts.dto;

import java.time.OffsetDateTime;

public class AlertResponse {

    private Long alertId;
    private String alertType;
    private String severity;
    private String title;
    private String message;
    private String status;
    private String alertDomain;
    private OffsetDateTime createdAt;
    private OffsetDateTime acknowledgedAt;
    private OffsetDateTime resolvedAt;

    // getters/setters
    // =========================
    // GETTERS
    // =========================

    public Long getAlertId() {
        return alertId;
    }

    public String getAlertType() {
        return alertType;
    }

    public String getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public String getAlertDomain() {
        return alertDomain;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    // =========================
    // SETTERS
    // =========================

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(
            OffsetDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }

    public void setAcknowledgedAt(
            OffsetDateTime acknowledgedAt
    ) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public void setAlertDomain(String alertDomain) {
        this.alertDomain = alertDomain;
    }

    public void setResolvedAt(
            OffsetDateTime resolvedAt
    ) {
        this.resolvedAt = resolvedAt;
    }
}