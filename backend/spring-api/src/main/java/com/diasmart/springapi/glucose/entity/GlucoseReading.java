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

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "raw_event_id")
    private Long rawEventId;

    @Column(name = "glucose_value_mg_dl")
    private Double glucoseValueMgDl;

    @Column(name = "measured_at")
    private OffsetDateTime measuredAt;

    @Column(name = "source")
    private String source;

    @Column(name = "meal_context")
    private String mealContext;

    @Column(name = "glucometer_sequence_number")
    private Integer glucometerSequenceNumber;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    // =========================
    // GETTERS
    // =========================

    public Long getGlucoseReadingId() {
        return glucoseReadingId;
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

    public Double getGlucoseValueMgDl() {
        return glucoseValueMgDl;
    }

    public OffsetDateTime getMeasuredAt() {
        return measuredAt;
    }

    public String getSource() {
        return source;
    }

    public String getMealContext() {
        return mealContext;
    }

    public Integer getGlucometerSequenceNumber() {
        return glucometerSequenceNumber;
    }

    public String getNotes() {
        return notes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    // =========================
    // SETTERS
    // =========================

    public void setGlucoseReadingId(
            Long glucoseReadingId
    ) {
        this.glucoseReadingId = glucoseReadingId;
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

    public void setGlucoseValueMgDl(
            Double glucoseValueMgDl
    ) {
        this.glucoseValueMgDl = glucoseValueMgDl;
    }

    public void setMeasuredAt(
            OffsetDateTime measuredAt
    ) {
        this.measuredAt = measuredAt;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setMealContext(String mealContext) {
        this.mealContext = mealContext;
    }

    public void setGlucometerSequenceNumber(
            Integer glucometerSequenceNumber
    ) {
        this.glucometerSequenceNumber =
                glucometerSequenceNumber;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}