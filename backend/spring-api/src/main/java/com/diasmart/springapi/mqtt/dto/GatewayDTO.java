package com.diasmart.springapi.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewayDTO {

    private String deviceUid;

    private String firmwareVersion;

    // =========================
    // GETTERS
    // =========================

    public String getDeviceUid() {
        return deviceUid;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    // =========================
    // SETTERS
    // =========================

    public void setDeviceUid(String deviceUid) {
        this.deviceUid = deviceUid;
    }

    public void setFirmwareVersion(
            String firmwareVersion
    ) {
        this.firmwareVersion = firmwareVersion;
    }
}
