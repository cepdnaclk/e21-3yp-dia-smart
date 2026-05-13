package com.diasmart.springapi.devices.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "device_health_logs")
public class DeviceHealthLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "health_log_id")
    private Long healthLogId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "raw_event_id")
    private Long rawEventId;

    @Column(name = "measured_at")
    private OffsetDateTime measuredAt;

    @Column(name = "battery_percent")
    private Double batteryPercent;

    @Column(name = "battery_voltage_v")
    private Double batteryVoltageV;

    @Column(name = "power_source")
    private String powerSource;

    @Column(name = "wifi_rssi_dbm")
    private Integer wifiRssiDbm;

    @Column(name = "ble_rssi_dbm")
    private Integer bleRssiDbm;

    @Column(name = "free_heap_bytes")
    private Integer freeHeapBytes;

    @Column(name = "firmware_version")
    private String firmwareVersion;

    @Column(name = "is_online")
    private Boolean online;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Long getHealthLogId() {
        return healthLogId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public Long getRawEventId() {
        return rawEventId;
    }

    public OffsetDateTime getMeasuredAt() {
        return measuredAt;
    }

    public Double getBatteryPercent() {
        return batteryPercent;
    }

    public Double getBatteryVoltageV() {
        return batteryVoltageV;
    }

    public String getPowerSource() {
        return powerSource;
    }

    public Integer getWifiRssiDbm() {
        return wifiRssiDbm;
    }

    public Integer getBleRssiDbm() {
        return bleRssiDbm;
    }

    public Integer getFreeHeapBytes() {
        return freeHeapBytes;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public Boolean getOnline() {
        return online;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setHealthLogId(Long healthLogId) {
        this.healthLogId = healthLogId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public void setRawEventId(Long rawEventId) {
        this.rawEventId = rawEventId;
    }

    public void setMeasuredAt(OffsetDateTime measuredAt) {
        this.measuredAt = measuredAt;
    }

    public void setBatteryPercent(Double batteryPercent) {
        this.batteryPercent = batteryPercent;
    }

    public void setBatteryVoltageV(Double batteryVoltageV) {
        this.batteryVoltageV = batteryVoltageV;
    }

    public void setPowerSource(String powerSource) {
        this.powerSource = powerSource;
    }

    public void setWifiRssiDbm(Integer wifiRssiDbm) {
        this.wifiRssiDbm = wifiRssiDbm;
    }

    public void setBleRssiDbm(Integer bleRssiDbm) {
        this.bleRssiDbm = bleRssiDbm;
    }

    public void setFreeHeapBytes(Integer freeHeapBytes) {
        this.freeHeapBytes = freeHeapBytes;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
