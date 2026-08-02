package com.diasmart.springapi.devices.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "device_activation_attempts")
public class DeviceActivationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activation_attempt_id")
    private Long activationAttemptId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "kit_id")
    private Long kitId;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_category", length = 40)
    private DeviceActivationFailureCategory failureCategory;

    @Column(name = "request_fingerprint", length = 64)
    private String requestFingerprint;

    @Column(name = "attempted_at", nullable = false)
    private OffsetDateTime attemptedAt;

    @Column(name = "blocked_until")
    private OffsetDateTime blockedUntil;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (attemptedAt == null) {
            attemptedAt = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }
    }

    public Long getActivationAttemptId() {
        return activationAttemptId;
    }

    public void setActivationAttemptId(Long activationAttemptId) {
        this.activationAttemptId = activationAttemptId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getKitId() {
        return kitId;
    }

    public void setKitId(Long kitId) {
        this.kitId = kitId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public DeviceActivationFailureCategory getFailureCategory() {
        return failureCategory;
    }

    public void setFailureCategory(DeviceActivationFailureCategory failureCategory) {
        this.failureCategory = failureCategory;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public void setRequestFingerprint(String requestFingerprint) {
        this.requestFingerprint = requestFingerprint;
    }

    public OffsetDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(OffsetDateTime attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public OffsetDateTime getBlockedUntil() {
        return blockedUntil;
    }

    public void setBlockedUntil(OffsetDateTime blockedUntil) {
        this.blockedUntil = blockedUntil;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
