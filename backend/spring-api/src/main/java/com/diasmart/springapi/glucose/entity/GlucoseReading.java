package com.diasmart.springapi.glucose.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
@Entity
@Table(name = "glucose_readings")
public class GlucoseReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long glucoseReadingId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "glucose_value_mg_dl")
    private Double glucoseValueMgDl;
    @Column(name = "measured_at")
    private OffsetDateTime measuredAt;

    // =========================
    // GETTERS
    // =========================

    public Long getGlucoseReadingId() {
        return glucoseReadingId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public Double getGlucoseValueMgDl() {
        return glucoseValueMgDl;
    }
    public OffsetDateTime getMeasuredAt() {
        return measuredAt;
    }

    // =========================
    // SETTERS
    // =========================

    public void setGlucoseReadingId(Long glucoseReadingId) {
        this.glucoseReadingId = glucoseReadingId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public void setGlucoseValueMgDl(Double glucoseValueMgDl) {
        this.glucoseValueMgDl = glucoseValueMgDl;
    }
    public void setMeasuredAt(
            OffsetDateTime measuredAt
    ) {
        this.measuredAt = measuredAt;
    }
}