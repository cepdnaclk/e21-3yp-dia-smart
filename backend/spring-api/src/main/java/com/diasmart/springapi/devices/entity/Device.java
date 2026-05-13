package com.diasmart.springapi.devices.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "device_uid")
    private String deviceUid;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "communication_type")
    private String communicationType;

    @Column(name = "firmware_version")
    private String firmwareVersion;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    @Column(name = "is_active")
    private Boolean active;

    public Long getDeviceId() {
        return deviceId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public String getDeviceUid() {
        return deviceUid;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getCommunicationType() {
        return communicationType;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public OffsetDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public void setDeviceUid(String deviceUid) {
        this.deviceUid = deviceUid;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setCommunicationType(String communicationType) {
        this.communicationType = communicationType;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public void setLastSeenAt(OffsetDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
