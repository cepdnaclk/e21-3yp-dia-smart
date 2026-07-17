package com.diasmart.springapi.deviceevents.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "device_sync_requests")
public class DeviceSyncRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sync_request_id")
    private Long syncRequestId;

    @Column(name = "event_id", length = 120)
    private String eventId;

    @Column(name = "outer_device_id")
    private Long outerDeviceId;

    @Column(name = "outer_device_uid", nullable = false, length = 80)
    private String outerDeviceUid;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "request_type", nullable = false, length = 60)
    private String requestType;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public Long getSyncRequestId() {
        return syncRequestId;
    }

    public void setSyncRequestId(Long syncRequestId) {
        this.syncRequestId = syncRequestId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
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

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
