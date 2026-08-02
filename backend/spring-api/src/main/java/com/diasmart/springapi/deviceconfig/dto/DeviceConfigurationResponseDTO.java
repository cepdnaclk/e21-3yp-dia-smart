package com.diasmart.springapi.deviceconfig.dto;

import java.time.OffsetDateTime;

public class DeviceConfigurationResponseDTO {

    private Long configurationId;
    private Long outerDeviceId;
    private Long patientId;
    private Long innerDeviceId;
    private Long penDeviceId;
    private Long glucometerDeviceId;
    private String wifiSsid;
    private String configurationStatus;
    private String outerUnitStatus;
    private String innerUnitStatus;
    private String innerUnitIpAddress;
    private String innerUnitMessage;
    private OffsetDateTime lastInnerUnitStatusAt;
    private Integer configurationVersion;
    private OffsetDateTime lastSyncedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public DeviceConfigurationResponseDTO() {
    }

    public Long getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(Long configurationId) {
        this.configurationId = configurationId;
    }

    public Long getOuterDeviceId() {
        return outerDeviceId;
    }

    public void setOuterDeviceId(Long outerDeviceId) {
        this.outerDeviceId = outerDeviceId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getInnerDeviceId() {
        return innerDeviceId;
    }

    public void setInnerDeviceId(Long innerDeviceId) {
        this.innerDeviceId = innerDeviceId;
    }

    public Long getPenDeviceId() {
        return penDeviceId;
    }

    public void setPenDeviceId(Long penDeviceId) {
        this.penDeviceId = penDeviceId;
    }

    public Long getGlucometerDeviceId() {
        return glucometerDeviceId;
    }

    public void setGlucometerDeviceId(Long glucometerDeviceId) {
        this.glucometerDeviceId = glucometerDeviceId;
    }

    public String getWifiSsid() {
        return wifiSsid;
    }

    public void setWifiSsid(String wifiSsid) {
        this.wifiSsid = wifiSsid;
    }

    public String getConfigurationStatus() {
        return configurationStatus;
    }

    public void setConfigurationStatus(String configurationStatus) {
        this.configurationStatus = configurationStatus;
    }

    public String getOuterUnitStatus() {
        return outerUnitStatus;
    }

    public void setOuterUnitStatus(String outerUnitStatus) {
        this.outerUnitStatus = outerUnitStatus;
    }

    public String getInnerUnitStatus() {
        return innerUnitStatus;
    }

    public void setInnerUnitStatus(String innerUnitStatus) {
        this.innerUnitStatus = innerUnitStatus;
    }

    public String getInnerUnitIpAddress() {
        return innerUnitIpAddress;
    }

    public void setInnerUnitIpAddress(String innerUnitIpAddress) {
        this.innerUnitIpAddress = innerUnitIpAddress;
    }

    public String getInnerUnitMessage() {
        return innerUnitMessage;
    }

    public void setInnerUnitMessage(String innerUnitMessage) {
        this.innerUnitMessage = innerUnitMessage;
    }

    public OffsetDateTime getLastInnerUnitStatusAt() {
        return lastInnerUnitStatusAt;
    }

    public void setLastInnerUnitStatusAt(OffsetDateTime lastInnerUnitStatusAt) {
        this.lastInnerUnitStatusAt = lastInnerUnitStatusAt;
    }

    public Integer getConfigurationVersion() {
        return configurationVersion;
    }

    public void setConfigurationVersion(Integer configurationVersion) {
        this.configurationVersion = configurationVersion;
    }

    public OffsetDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(OffsetDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
