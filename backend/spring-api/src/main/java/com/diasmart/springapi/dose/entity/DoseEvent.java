package com.diasmart.springapi.dose.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


// Maps this class to dose_events table
@Entity

// Database table name
@Table(name = "dose_events")

@Getter
@Setter
public class DoseEvent {

    // Primary key of dose event
    @Id

    // Auto-increment ID
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    // Maps to dose_event_id column
    @Column(name = "dose_event_id")
    private Long doseEventId;


    // Patient who received insulin
    @Column(name = "patient_id")
    private Long patientId;


    // Dose timestamp
    @Column(name = "injected_at")
    private OffsetDateTime injectedAt;


    // Insulin dose amount
    @Column(name = "dose_units")
    private Double doseUnits;


    // Detection method used
    @Column(name = "detection_method")
    private String detectionMethod;


    // Event confirmation status
    @Column(name = "event_status")
    private String eventStatus;
}