package com.diasmart.springapi.deviceconfig.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "device_configurations")
public class DeviceConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "configuration_id")
    private Long configurationId;

    @Column(name = "outer_device_id", nullable = false, unique = true)
    private Long outerDeviceId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "inner_device_id")
    private Long innerDeviceId;

    @Column(name = "pen_device_id")
    private Long penDeviceId;

    @Column(name = "glucometer_device_id")
    private Long glucometerDeviceId;

    @Column(name = "wifi_ssid", nullable = false, length = 100)
    private String wifiSsid;

    @Column(name = "wifi_password", nullable = false, columnDefinition = "TEXT")
    private String wifiPassword;

    @Column(name = "wifi_password_ciphertext", columnDefinition = "TEXT")
    private String wifiPasswordCiphertext;

    @Column(name = "wifi_password_nonce", length = 64)
    private String wifiPasswordNonce;

    @Column(name = "wifi_password_tag", length = 64)
    private String wifiPasswordTag;

    @Column(name = "configuration_status", length = 20)
    private String configurationStatus;

    @Column(name = "outer_unit_status", length = 30)
    private String outerUnitStatus;

    @Column(name = "inner_unit_status", length = 30)
    private String innerUnitStatus;

    @Column(name = "inner_unit_ip_address", length = 64)
    private String innerUnitIpAddress;

    @Column(name = "inner_unit_message", columnDefinition = "TEXT")
    private String innerUnitMessage;

    @Column(name = "last_inner_unit_status_at")
    private OffsetDateTime lastInnerUnitStatusAt;

    @Column(name = "configuration_version", nullable = false)
    private Integer configurationVersion;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public DeviceConfiguration() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (configurationStatus == null) {
            configurationStatus = "PENDING";
        }
        if (configurationVersion == null) {
            configurationVersion = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
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

    public String getWifiPassword() {
        return wifiPassword;
    }

    public void setWifiPassword(String wifiPassword) {
        this.wifiPassword = wifiPassword;
    }

    public String getWifiPasswordCiphertext() {
        return wifiPasswordCiphertext;
    }

    public void setWifiPasswordCiphertext(String wifiPasswordCiphertext) {
        this.wifiPasswordCiphertext = wifiPasswordCiphertext;
    }

    public String getWifiPasswordNonce() {
        return wifiPasswordNonce;
    }

    public void setWifiPasswordNonce(String wifiPasswordNonce) {
        this.wifiPasswordNonce = wifiPasswordNonce;
    }

    public String getWifiPasswordTag() {
        return wifiPasswordTag;
    }

    public void setWifiPasswordTag(String wifiPasswordTag) {
        this.wifiPasswordTag = wifiPasswordTag;
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
