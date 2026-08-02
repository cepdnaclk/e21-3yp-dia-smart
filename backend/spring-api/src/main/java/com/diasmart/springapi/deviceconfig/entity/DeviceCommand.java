package com.diasmart.springapi.deviceconfig.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "device_commands")
public class DeviceCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "command_id")
    private Long commandId;

    @Column(name = "command_uid", unique = true, length = 80)
    private String commandUid;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "device_configuration_id")
    private Long deviceConfigurationId;

    @Column(name = "configuration_version")
    private Integer configurationVersion;

    @Column(name = "command_type", nullable = false, length = 40)
    private String commandType;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "JSONB")
    private String payload;

    @Column(name = "command_status", length = 20)
    private String commandStatus;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "last_attempt_at")
    private OffsetDateTime lastAttemptAt;

    @Column(name = "next_retry_at")
    private OffsetDateTime nextRetryAt;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;

    @Column(name = "timeout_at")
    private OffsetDateTime timeoutAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    public DeviceCommand() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (commandStatus == null) {
            commandStatus = "PENDING";
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    public Long getCommandId() {
        return commandId;
    }

    public void setCommandId(Long commandId) {
        this.commandId = commandId;
    }

    public String getCommandUid() {
        return commandUid;
    }

    public void setCommandUid(String commandUid) {
        this.commandUid = commandUid;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getDeviceConfigurationId() {
        return deviceConfigurationId;
    }

    public void setDeviceConfigurationId(Long deviceConfigurationId) {
        this.deviceConfigurationId = deviceConfigurationId;
    }

    public Integer getConfigurationVersion() {
        return configurationVersion;
    }

    public void setConfigurationVersion(Integer configurationVersion) {
        this.configurationVersion = configurationVersion;
    }

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getCommandStatus() {
        return commandStatus;
    }

    public void setCommandStatus(String commandStatus) {
        this.commandStatus = commandStatus;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public OffsetDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(OffsetDateTime lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public OffsetDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(OffsetDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public OffsetDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(OffsetDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public OffsetDateTime getTimeoutAt() {
        return timeoutAt;
    }

    public void setTimeoutAt(OffsetDateTime timeoutAt) {
        this.timeoutAt = timeoutAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
