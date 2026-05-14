package com.diasmart.springapi.devices.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterDeviceRequestDTO {

    @Size(max = 80)
    private String deviceUid;

    @Size(max = 80)
    private String deviceId;

    @Size(max = 128)
    private String awsThingName;

    @Size(max = 128)
    private String mqttClientId;

    @Size(max = 40)
    private String macAddress;

    @Size(max = 80)
    private String serialNumber;

    @NotBlank
    @Size(max = 30)
    private String deviceType;

    @Size(max = 120)
    private String deviceName;

    @Size(max = 30)
    private String communicationType;

    @Size(max = 50)
    private String firmwareVersion;

    @Size(max = 50)
    private String hardwareVersion;

    private String notes;

    public String getDeviceUid() {
        return deviceUid;
    }

    public void setDeviceUid(String deviceUid) {
        this.deviceUid = deviceUid;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getAwsThingName() {
        return awsThingName;
    }

    public void setAwsThingName(String awsThingName) {
        this.awsThingName = awsThingName;
    }

    public String getMqttClientId() {
        return mqttClientId;
    }

    public void setMqttClientId(String mqttClientId) {
        this.mqttClientId = mqttClientId;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
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

    public String getCommunicationType() {
        return communicationType;
    }

    public void setCommunicationType(String communicationType) {
        this.communicationType = communicationType;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
