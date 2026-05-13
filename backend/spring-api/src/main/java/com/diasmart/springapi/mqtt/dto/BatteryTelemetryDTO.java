package com.diasmart.springapi.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BatteryTelemetryDTO {

    private String innerUnitDeviceUid;

    private String penUnitDeviceUid;

    private String outerUnitDeviceUid;

    private Integer innerUnitPercent;

    private Integer penUnitPercent;

    private Integer outerUnitPercent;

    private Double innerUnitVoltageV;

    private Double penUnitVoltageV;

    private Double outerUnitVoltageV;

    private Integer wifiRssiDbm;

    private Integer bleRssiDbm;

    private Integer freeHeapBytes;

    private String powerSource;

    private String status;

    // =========================
    // GETTERS
    // =========================

    public String getInnerUnitDeviceUid() {
        return innerUnitDeviceUid;
    }

    public String getPenUnitDeviceUid() {
        return penUnitDeviceUid;
    }

    public String getOuterUnitDeviceUid() {
        return outerUnitDeviceUid;
    }

    public Integer getInnerUnitPercent() {
        return innerUnitPercent;
    }

    public Integer getPenUnitPercent() {
        return penUnitPercent;
    }

    public Integer getOuterUnitPercent() {
        return outerUnitPercent;
    }

    public Double getInnerUnitVoltageV() {
        return innerUnitVoltageV;
    }

    public Double getPenUnitVoltageV() {
        return penUnitVoltageV;
    }

    public Double getOuterUnitVoltageV() {
        return outerUnitVoltageV;
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

    public String getPowerSource() {
        return powerSource;
    }

    public String getStatus() {
        return status;
    }

    // =========================
    // SETTERS
    // =========================

    public void setInnerUnitDeviceUid(
            String innerUnitDeviceUid
    ) {
        this.innerUnitDeviceUid = innerUnitDeviceUid;
    }

    public void setPenUnitDeviceUid(
            String penUnitDeviceUid
    ) {
        this.penUnitDeviceUid = penUnitDeviceUid;
    }

    public void setOuterUnitDeviceUid(
            String outerUnitDeviceUid
    ) {
        this.outerUnitDeviceUid = outerUnitDeviceUid;
    }

    public void setInnerUnitPercent(
            Integer innerUnitPercent
    ) {
        this.innerUnitPercent = innerUnitPercent;
    }

    public void setPenUnitPercent(
            Integer penUnitPercent
    ) {
        this.penUnitPercent = penUnitPercent;
    }

    public void setOuterUnitPercent(
            Integer outerUnitPercent
    ) {
        this.outerUnitPercent = outerUnitPercent;
    }

    public void setInnerUnitVoltageV(
            Double innerUnitVoltageV
    ) {
        this.innerUnitVoltageV = innerUnitVoltageV;
    }

    public void setPenUnitVoltageV(
            Double penUnitVoltageV
    ) {
        this.penUnitVoltageV = penUnitVoltageV;
    }

    public void setOuterUnitVoltageV(
            Double outerUnitVoltageV
    ) {
        this.outerUnitVoltageV = outerUnitVoltageV;
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

    public void setPowerSource(String powerSource) {
        this.powerSource = powerSource;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
