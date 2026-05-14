package com.diasmart.springapi.devices.dto;

import java.time.OffsetDateTime;

public class DeviceDiagnosticsDTO {

    private Long deviceId;
    private String deviceUid;
    private String deviceType;
    private String deviceName;
    private String status;
    private Boolean online;
    private String firmwareVersion;
    private String hardwareVersion;
    private OffsetDateTime lastMqttReceivedAt;
    private OffsetDateTime lastSeenAt;
    private OffsetDateTime latestHealthAt;
    private Double batteryPercent;
    private Double batteryVoltageV;
    private String powerSource;
    private Integer wifiRssiDbm;
    private Integer bleRssiDbm;
    private Integer freeHeapBytes;
    private DeviceReplayStatisticsDTO replayStatistics;

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
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

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getHardwareVersion() {
        return hardwareVersion;
    }

    public void setHardwareVersion(String hardwareVersion) {
        this.hardwareVersion = hardwareVersion;
    }

    public OffsetDateTime getLastMqttReceivedAt() {
        return lastMqttReceivedAt;
    }

    public void setLastMqttReceivedAt(
            OffsetDateTime lastMqttReceivedAt
    ) {
        this.lastMqttReceivedAt = lastMqttReceivedAt;
    }

    public OffsetDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(OffsetDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public OffsetDateTime getLatestHealthAt() {
        return latestHealthAt;
    }

    public void setLatestHealthAt(OffsetDateTime latestHealthAt) {
        this.latestHealthAt = latestHealthAt;
    }

    public Double getBatteryPercent() {
        return batteryPercent;
    }

    public void setBatteryPercent(Double batteryPercent) {
        this.batteryPercent = batteryPercent;
    }

    public Double getBatteryVoltageV() {
        return batteryVoltageV;
    }

    public void setBatteryVoltageV(Double batteryVoltageV) {
        this.batteryVoltageV = batteryVoltageV;
    }

    public String getPowerSource() {
        return powerSource;
    }

    public void setPowerSource(String powerSource) {
        this.powerSource = powerSource;
    }

    public Integer getWifiRssiDbm() {
        return wifiRssiDbm;
    }

    public void setWifiRssiDbm(Integer wifiRssiDbm) {
        this.wifiRssiDbm = wifiRssiDbm;
    }

    public Integer getBleRssiDbm() {
        return bleRssiDbm;
    }

    public void setBleRssiDbm(Integer bleRssiDbm) {
        this.bleRssiDbm = bleRssiDbm;
    }

    public Integer getFreeHeapBytes() {
        return freeHeapBytes;
    }

    public void setFreeHeapBytes(Integer freeHeapBytes) {
        this.freeHeapBytes = freeHeapBytes;
    }

    public DeviceReplayStatisticsDTO getReplayStatistics() {
        return replayStatistics;
    }

    public void setReplayStatistics(
            DeviceReplayStatisticsDTO replayStatistics
    ) {
        this.replayStatistics = replayStatistics;
    }
}
