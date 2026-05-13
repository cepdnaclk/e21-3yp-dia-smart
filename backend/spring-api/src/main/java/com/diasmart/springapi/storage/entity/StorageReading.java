package com.diasmart.springapi.storage.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


// Maps this class to storage_readings table
@Entity

// Database table name
@Table(name = "storage_readings")

@Getter
@Setter
public class StorageReading {

    // Primary key
    @Id

    // Auto-generated ID
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    // Maps storage_reading_id column
    @Column(name = "storage_reading_id")
    private Long storageReadingId;


    // Patient ID
    @Column(name = "patient_id")
    private Long patientId;


    // Timestamp of reading
    @Column(name = "measured_at")
    private OffsetDateTime measuredAt;


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
}