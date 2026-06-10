package com.diasmart.springapi.storage.dto;

import java.time.OffsetDateTime;

public class StorageReadingResponse {

    private Long storageReadingId;

    private Double temperatureC;

    private Double humidityPercent;

    private String doorState;

    private Integer doorOpenDurationSeconds;

    private String temperatureStatus;

    private String notes;

    private OffsetDateTime measuredAt;

    private OffsetDateTime createdAt;

    public Long getStorageReadingId() {
        return storageReadingId;
    }

    public void setStorageReadingId(Long storageReadingId) {
        this.storageReadingId = storageReadingId;
    }

    public Double getTemperatureC() {
        return temperatureC;
    }

    public void setTemperatureC(Double temperatureC) {
        this.temperatureC = temperatureC;
    }

    public Double getHumidityPercent() {
        return humidityPercent;
    }

    public void setHumidityPercent(Double humidityPercent) {
        this.humidityPercent = humidityPercent;
    }

    public String getDoorState() {
        return doorState;
    }

    public void setDoorState(String doorState) {
        this.doorState = doorState;
    }

    public Integer getDoorOpenDurationSeconds() {
        return doorOpenDurationSeconds;
    }

    public void setDoorOpenDurationSeconds(
            Integer doorOpenDurationSeconds
    ) {
        this.doorOpenDurationSeconds =
                doorOpenDurationSeconds;
    }

    public String getTemperatureStatus() {
        return temperatureStatus;
    }

    public void setTemperatureStatus(
            String temperatureStatus
    ) {
        this.temperatureStatus = temperatureStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public OffsetDateTime getMeasuredAt() {
        return measuredAt;
    }

    public void setMeasuredAt(
            OffsetDateTime measuredAt
    ) {
        this.measuredAt = measuredAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            OffsetDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }
}