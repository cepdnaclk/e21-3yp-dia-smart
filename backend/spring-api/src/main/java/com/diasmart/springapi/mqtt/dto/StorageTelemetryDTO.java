package com.diasmart.springapi.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StorageTelemetryDTO {

    private String deviceUid;

    private Double temperatureC;

    private Double humidityPercent;

    private String doorStatus;

    private Integer doorOpenDurationSeconds;

    private String temperatureStatus;

    private String notes;

    // =========================
    // GETTERS
    // =========================

    public String getDeviceUid() {
        return deviceUid;
    }

    public Double getTemperatureC() {
        return temperatureC;
    }

    public Double getHumidityPercent() {
        return humidityPercent;
    }

    public String getDoorStatus() {
        return doorStatus;
    }

    public Integer getDoorOpenDurationSeconds() {
        return doorOpenDurationSeconds;
    }

    public String getTemperatureStatus() {
        return temperatureStatus;
    }

    public String getNotes() {
        return notes;
    }

    // =========================
    // SETTERS
    // =========================

    public void setDeviceUid(String deviceUid) {
        this.deviceUid = deviceUid;
    }

    public void setTemperatureC(
            Double temperatureC
    ) {
        this.temperatureC = temperatureC;
    }

    public void setHumidityPercent(
            Double humidityPercent
    ) {
        this.humidityPercent = humidityPercent;
    }

    public void setDoorStatus(
            String doorStatus
    ) {
        this.doorStatus = doorStatus;
    }

    public void setDoorOpenDurationSeconds(
            Integer doorOpenDurationSeconds
    ) {
        this.doorOpenDurationSeconds =
                doorOpenDurationSeconds;
    }

    public void setTemperatureStatus(
            String temperatureStatus
    ) {
        this.temperatureStatus = temperatureStatus;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
