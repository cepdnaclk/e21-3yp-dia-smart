package com.diasmart.springapi.storage.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "storage_readings")
public class StorageReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
     @Column(name = "storage_reading_id")
    private Long storageReadingId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "raw_event_id")
    private Long rawEventId;

    @Column(name = "temperature_c")
    private Double temperatureC;

    @Column(name = "humidity_percent")
    private Double humidityPercent;

    @Column(name = "door_state")
    private String doorState;

    @Column(name = "door_open_duration_seconds")
    private Integer doorOpenDurationSeconds;

    @Column(name = "temperature_status")
    private String temperatureStatus;

    @Column(name = "notes")
    private String notes;

    // Database insertion time
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    // Actual sensor measurement time
    @Column(name = "measured_at")
    private OffsetDateTime measuredAt; 

    // =========================
    // GETTERS
    // =========================

    public Long getStorageReadingId() {
        return storageReadingId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public Long getRawEventId() {
        return rawEventId;
    }

    public Double getTemperatureC() {
        return temperatureC;
    }

    public Double getHumidityPercent() {
        return humidityPercent;
    }

    public String getDoorState() {
        return doorState;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    public OffsetDateTime getMeasuredAt() {
        return measuredAt;
    }

    // =========================
    // SETTERS
    // =========================

    public void setStorageReadingId(
            Long storageReadingId
    ) {
        this.storageReadingId = storageReadingId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public void setRawEventId(Long rawEventId) {
        this.rawEventId = rawEventId;
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

    public void setDoorState(
            String doorState
    ) {
        this.doorState = doorState;
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

    public void setCreatedAt(
            OffsetDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }

    public void setMeasuredAt(
            OffsetDateTime measuredAt
    ) {
        this.measuredAt = measuredAt;
    }
}
