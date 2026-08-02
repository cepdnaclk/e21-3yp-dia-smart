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
    private String outerDeviceUid;
    private Long lastSuccessfulConfigurationId;
    private Integer lastSuccessfulConfigurationVersion;
    private OffsetDateTime lastSuccessfulAt;
    private Long previousConfigurationId;
    private Integer previousConfigurationVersion;
    private OffsetDateTime provisioningStartedAt;
    private OffsetDateTime provisioningCompletedAt;
    private OffsetDateTime provisioningTimeoutAt;
    private String provisioningFailureCode;
    private String provisioningFailureMessage;
    private String rollbackStatus;
    private String mqttStatus;
    private Long lastProvisioningCommandId;
    private String lastProvisioningCommandUid;
    private Long commandNumericId;
    private String commandId;
    private String commandStatus;
    private OffsetDateTime publishedAt;
    private OffsetDateTime lastAcknowledgedAt;
    private OffsetDateTime timeoutAt;
    private OffsetDateTime completedAt;
    private String overallStatus;
    private String lastErrorCode;
    private String lastErrorMessage;
    private Boolean terminal;
    private String lastAckStatus;
    private String lastAckProcessingResult;
    private String lastResultProcessingStatus;
    private String lastResultProcessingResult;
    private Boolean staleResultIgnored;
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

    public String getOuterDeviceUid() {
        return outerDeviceUid;
    }

    public void setOuterDeviceUid(String outerDeviceUid) {
        this.outerDeviceUid = outerDeviceUid;
    }

    public Long getLastSuccessfulConfigurationId() {
        return lastSuccessfulConfigurationId;
    }

    public void setLastSuccessfulConfigurationId(Long lastSuccessfulConfigurationId) {
        this.lastSuccessfulConfigurationId = lastSuccessfulConfigurationId;
    }

    public Integer getLastSuccessfulConfigurationVersion() {
        return lastSuccessfulConfigurationVersion;
    }

    public void setLastSuccessfulConfigurationVersion(Integer lastSuccessfulConfigurationVersion) {
        this.lastSuccessfulConfigurationVersion = lastSuccessfulConfigurationVersion;
    }

    public OffsetDateTime getLastSuccessfulAt() {
        return lastSuccessfulAt;
    }

    public void setLastSuccessfulAt(OffsetDateTime lastSuccessfulAt) {
        this.lastSuccessfulAt = lastSuccessfulAt;
    }

    public Long getPreviousConfigurationId() {
        return previousConfigurationId;
    }

    public void setPreviousConfigurationId(Long previousConfigurationId) {
        this.previousConfigurationId = previousConfigurationId;
    }

    public Integer getPreviousConfigurationVersion() {
        return previousConfigurationVersion;
    }

    public void setPreviousConfigurationVersion(Integer previousConfigurationVersion) {
        this.previousConfigurationVersion = previousConfigurationVersion;
    }

    public OffsetDateTime getProvisioningStartedAt() {
        return provisioningStartedAt;
    }

    public void setProvisioningStartedAt(OffsetDateTime provisioningStartedAt) {
        this.provisioningStartedAt = provisioningStartedAt;
    }

    public OffsetDateTime getProvisioningCompletedAt() {
        return provisioningCompletedAt;
    }

    public void setProvisioningCompletedAt(OffsetDateTime provisioningCompletedAt) {
        this.provisioningCompletedAt = provisioningCompletedAt;
    }

    public OffsetDateTime getProvisioningTimeoutAt() {
        return provisioningTimeoutAt;
    }

    public void setProvisioningTimeoutAt(OffsetDateTime provisioningTimeoutAt) {
        this.provisioningTimeoutAt = provisioningTimeoutAt;
    }

    public String getProvisioningFailureCode() {
        return provisioningFailureCode;
    }

    public void setProvisioningFailureCode(String provisioningFailureCode) {
        this.provisioningFailureCode = provisioningFailureCode;
    }

    public String getProvisioningFailureMessage() {
        return provisioningFailureMessage;
    }

    public void setProvisioningFailureMessage(String provisioningFailureMessage) {
        this.provisioningFailureMessage = provisioningFailureMessage;
    }

    public String getRollbackStatus() {
        return rollbackStatus;
    }

    public void setRollbackStatus(String rollbackStatus) {
        this.rollbackStatus = rollbackStatus;
    }

    public String getMqttStatus() {
        return mqttStatus;
    }

    public void setMqttStatus(String mqttStatus) {
        this.mqttStatus = mqttStatus;
    }

    public Long getLastProvisioningCommandId() {
        return lastProvisioningCommandId;
    }

    public void setLastProvisioningCommandId(Long lastProvisioningCommandId) {
        this.lastProvisioningCommandId = lastProvisioningCommandId;
    }

    public String getLastProvisioningCommandUid() {
        return lastProvisioningCommandUid;
    }

    public void setLastProvisioningCommandUid(String lastProvisioningCommandUid) {
        this.lastProvisioningCommandUid = lastProvisioningCommandUid;
    }

    public Long getCommandNumericId() {
        return commandNumericId;
    }

    public void setCommandNumericId(Long commandNumericId) {
        this.commandNumericId = commandNumericId;
    }

    public String getCommandId() {
        return commandId;
    }

    public void setCommandId(String commandId) {
        this.commandId = commandId;
    }

    public String getCommandStatus() {
        return commandStatus;
    }

    public void setCommandStatus(String commandStatus) {
        this.commandStatus = commandStatus;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public OffsetDateTime getLastAcknowledgedAt() {
        return lastAcknowledgedAt;
    }

    public void setLastAcknowledgedAt(OffsetDateTime lastAcknowledgedAt) {
        this.lastAcknowledgedAt = lastAcknowledgedAt;
    }

    public OffsetDateTime getTimeoutAt() {
        return timeoutAt;
    }

    public void setTimeoutAt(OffsetDateTime timeoutAt) {
        this.timeoutAt = timeoutAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public void setLastErrorCode(String lastErrorCode) {
        this.lastErrorCode = lastErrorCode;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public Boolean getTerminal() {
        return terminal;
    }

    public void setTerminal(Boolean terminal) {
        this.terminal = terminal;
    }

    public String getLastAckStatus() {
        return lastAckStatus;
    }

    public void setLastAckStatus(String lastAckStatus) {
        this.lastAckStatus = lastAckStatus;
    }

    public String getLastAckProcessingResult() {
        return lastAckProcessingResult;
    }

    public void setLastAckProcessingResult(String lastAckProcessingResult) {
        this.lastAckProcessingResult = lastAckProcessingResult;
    }

    public String getLastResultProcessingStatus() {
        return lastResultProcessingStatus;
    }

    public void setLastResultProcessingStatus(String lastResultProcessingStatus) {
        this.lastResultProcessingStatus = lastResultProcessingStatus;
    }

    public String getLastResultProcessingResult() {
        return lastResultProcessingResult;
    }

    public void setLastResultProcessingResult(String lastResultProcessingResult) {
        this.lastResultProcessingResult = lastResultProcessingResult;
    }

    public Boolean getStaleResultIgnored() {
        return staleResultIgnored;
    }

    public void setStaleResultIgnored(Boolean staleResultIgnored) {
        this.staleResultIgnored = staleResultIgnored;
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
