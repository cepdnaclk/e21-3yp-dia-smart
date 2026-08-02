package com.diasmart.springapi.mqtt.dto;

import java.time.Instant;

public class CommandAckDTO {

    private String commandId;
    private String commandType;
    private String outerDeviceId;
    private String status;
    private String message;
    private Integer configurationVersion;
    private Instant timestamp;

    public CommandAckDTO() {
    }

    public String getCommandId() {
        return commandId;
    }

    public void setCommandId(String commandId) {
        this.commandId = commandId;
    }

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

    public String getOuterDeviceId() {
        return outerDeviceId;
    }

    public void setOuterDeviceId(String outerDeviceId) {
        this.outerDeviceId = outerDeviceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getConfigurationVersion() {
        return configurationVersion;
    }

    public void setConfigurationVersion(Integer configurationVersion) {
        this.configurationVersion = configurationVersion;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
