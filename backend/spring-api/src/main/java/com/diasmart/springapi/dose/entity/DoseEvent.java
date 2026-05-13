package com.diasmart.springapi.dose.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


// Maps dose_events table
@Entity
@Table(name = "dose_events")

@Getter
@Setter
public class DoseEvent {

    // Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "dose_event_id")
    private Long doseEventId;


    // Related patient
    @Column(name = "patient_id")
    private Long patientId;


    // Related device
    @Column(name = "device_id")
    private Long deviceId;


    // Raw telemetry event reference
    @Column(name = "raw_event_id")
    private Long rawEventId;


    // Injection timestamp
    @Column(name = "injected_at")
    private OffsetDateTime injectedAt;


    // Insulin dose amount
    @Column(name = "dose_units")
    private Double doseUnits;


    // Detection source
    @Column(name = "detection_method")
    private String detectionMethod;


    // Pen rotation angle
    @Column(name = "angle_degrees")
    private Double angleDegrees;


    // Detection confidence
    @Column(name = "confidence_percent")
    private Double confidencePercent;


    // Event status
    @Column(name = "event_status")
    private String eventStatus;


    // Additional notes
    @Column(name = "notes")
    private String notes;


    // Record creation time
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}