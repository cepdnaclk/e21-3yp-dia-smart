package com.diasmart.springapi.devices.dto;

import java.time.OffsetDateTime;

public class DeviceSummaryDTO {

    private Long deviceId;
    private Long patientId;
    private String deviceUid;
    private String deviceType;
    private String deviceName;
    private String status;
    private Boolean online;
    private Double batteryPercent;
    private OffsetDateTime lastSeenAt;
    private Boolean active;

    private com.diasmart.springapi.devices.dto.DeviceResponseDTO.BuyerDTO buyer;

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

    public String getDeviceUid() {
        return deviceUid;
    }

    public void setDeviceUid(String deviceUid) {
        this.deviceUid = deviceUid;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public Double getBatteryPercent() {
        return batteryPercent;
    }

    public void setBatteryPercent(Double batteryPercent) {
        this.batteryPercent = batteryPercent;
    }

    public OffsetDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(OffsetDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public com.diasmart.springapi.devices.dto.DeviceResponseDTO.BuyerDTO getBuyer() {
        return buyer;
    }

    public void setBuyer(com.diasmart.springapi.devices.dto.DeviceResponseDTO.BuyerDTO buyer) {
        this.buyer = buyer;
    }
}
