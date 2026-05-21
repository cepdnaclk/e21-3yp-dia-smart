package com.diasmart.springapi.alerts.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long alertId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "raw_event_id")
    private Long rawEventId;

    @Column(name = "related_table")
    private String relatedTable;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "alert_type")
    private String alertType;

    @Column(name = "severity")
    private String severity;

    @Column(name = "title")
    private String title;

    @Column(name = "message")
    private String message;

    @Column(name = "dedupe_key")
    private String dedupeKey;

    @Column(name = "alert_domain")
    private String alertDomain;

    @Column(name = "status")
    private String status;

    @Column(name = "first_detected_at")
    private OffsetDateTime firstDetectedAt;

    @Column(name = "last_detected_at")
    private OffsetDateTime lastDetectedAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;

    @Column(name = "acknowledged_by")
    private Long acknowledgedBy;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolution_note")
    private String resolutionNote;

    // Generate getters/setters
    // =========================
    // GETTERS
    // =========================

    public Long getAlertId() {
        return alertId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public Long getRawEventId() {
        return rawEventId;
    }

    public String getRelatedTable() {
        return relatedTable;
    }

    public Long getRelatedId() {
        return relatedId;
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

    public String getDedupeKey() {
        return dedupeKey;
    }

    public String getAlertDomain() {
        return alertDomain;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getFirstDetectedAt() {
        return firstDetectedAt;
    }

    public OffsetDateTime getLastDetectedAt() {
        return lastDetectedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public Long getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public Long getResolvedBy() {
        return resolvedBy;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    // =========================
    // SETTERS
    // =========================

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public void setRawEventId(Long rawEventId) {
        this.rawEventId = rawEventId;
    }

    public void setRelatedTable(String relatedTable) {
        this.relatedTable = relatedTable;
    }

    public void setRelatedId(Long relatedId) {
        this.relatedId = relatedId;
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

    public void setDedupeKey(String dedupeKey) {
        this.dedupeKey = dedupeKey;
    }

    public void setAlertDomain(String alertDomain) {
        this.alertDomain = alertDomain;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setFirstDetectedAt(
            OffsetDateTime firstDetectedAt
    ) {
        this.firstDetectedAt = firstDetectedAt;
    }

    public void setLastDetectedAt(
            OffsetDateTime lastDetectedAt
    ) {
        this.lastDetectedAt = lastDetectedAt;
    }

    public void setCreatedAt(
            OffsetDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(
            OffsetDateTime updatedAt
    ) {
        this.updatedAt = updatedAt;
    }

    public void setAcknowledgedAt(
            OffsetDateTime acknowledgedAt
    ) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public void setAcknowledgedBy(
            Long acknowledgedBy
    ) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public void setResolvedAt(
            OffsetDateTime resolvedAt
    ) {
        this.resolvedAt = resolvedAt;
    }

    public void setResolvedBy(
            Long resolvedBy
    ) {
        this.resolvedBy = resolvedBy;
    }

    public void setResolutionNote(
            String resolutionNote
    ) {
        this.resolutionNote = resolutionNote;
    }
    }