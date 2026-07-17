package com.diasmart.springapi.careplan.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

public class CarePlanResponse {

    private Long snapshotId;
    private String carePlanId;
    private Integer version;
    private Long patientId;
    private Long outerDeviceId;
    private String outerDeviceUid;
    private String timezone;
    private LocalDate effectiveFrom;
    private String status;
    private OffsetDateTime publishedAt;
    private OffsetDateTime acknowledgedAt;
    private Map<String, Object> payload;

    public Long getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(Long snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getCarePlanId() {
        return carePlanId;
    }

    public void setCarePlanId(String carePlanId) {
        this.carePlanId = carePlanId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getOuterDeviceId() {
        return outerDeviceId;
    }

    public void setOuterDeviceId(Long outerDeviceId) {
        this.outerDeviceId = outerDeviceId;
    }

    public String getOuterDeviceUid() {
        return outerDeviceUid;
    }

    public void setOuterDeviceUid(String outerDeviceUid) {
        this.outerDeviceUid = outerDeviceUid;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public OffsetDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(OffsetDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
