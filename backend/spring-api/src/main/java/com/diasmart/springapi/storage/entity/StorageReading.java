package com.diasmart.springapi.storage.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


// Maps storage_readings table
@Entity
@Table(name = "storage_readings")

@Getter
@Setter
public class StorageReading {

    // Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "storage_reading_id")
    private Long storageReadingId;


    // Related patient
    @Column(name = "patient_id")
    private Long patientId;


    // Related device
    @Column(name = "device_id")
    private Long deviceId;


    // Raw telemetry event reference
    @Column(name = "raw_event_id")
    private Long rawEventId;


    // Refrigerator temperature
    @Column(name = "temperature_c")
    private Double temperatureC;


    // Refrigerator humidity
    @Column(name = "humidity_percent")
    private Double humidityPercent;


    // Door state
    @Column(name = "door_state")
    private String doorState;


    // Door open duration
    @Column(name = "door_open_duration_seconds")
    private Integer doorOpenDurationSeconds;


    // Temperature safety status
    @Column(name = "temperature_status")
    private String temperatureStatus;


    // Additional notes
    @Column(name = "notes")
    private String notes;


    // Database insertion timestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;


    // Actual measurement timestamp
    @Column(name = "measured_at")
    private OffsetDateTime measuredAt;
}