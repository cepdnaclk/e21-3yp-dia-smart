package com.diasmart.springapi.devices.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "device_uid", unique = true, nullable = false)
    private String deviceUid;

    @Column(name = "aws_thing_name")
    private String awsThingName;

    @Column(name = "mqtt_client_id")
    private String mqttClientId;

    @Column(name = "mac_address")
    private String macAddress;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "communication_type")
    private String communicationType;

    @Column(name = "firmware_version")
    private String firmwareVersion;

    @Column(name = "hardware_version")
    private String hardwareVersion;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    @Column(name = "is_active")
    private Boolean active;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "buyer_id")
    private Long buyerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DeviceStatus status;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (active == null) {
            active = true;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public String getDeviceUid() {
        return deviceUid;
    }

    public String getAwsThingName() {
        return awsThingName;
    }

    public String getMqttClientId() {
        return mqttClientId;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getSerialNumber() {
        return serialNumber;
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

    public String getHardwareVersion() {
        return hardwareVersion;
    }

    public OffsetDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public Boolean getActive() {
        return active;
    }

    public String getNotes() {
        return notes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
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

    public void setAwsThingName(String awsThingName) {
        this.awsThingName = awsThingName;
    }

    public void setMqttClientId(String mqttClientId) {
        this.mqttClientId = mqttClientId;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
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

    public void setHardwareVersion(String hardwareVersion) {
        this.hardwareVersion = hardwareVersion;
    }

    public void setLastSeenAt(OffsetDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }
}
