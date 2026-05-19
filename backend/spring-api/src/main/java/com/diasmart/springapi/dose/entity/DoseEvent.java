package com.diasmart.springapi.dose.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "dose_events")
public class DoseEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dose_event_id")
    private Long doseEventId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "raw_event_id")
    private Long rawEventId;

    @Column(name = "prescription_id")
    private Long prescriptionId;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "injected_at")
    private OffsetDateTime injectedAt;

    @Column(name = "dose_units")
    private Double doseUnits;

    @Column(name = "detection_method")
    private String detectionMethod;

    @Column(name = "angle_degrees")
    private Double angleDegrees;

    @Column(name = "confidence_percent")
    private Double confidencePercent;

    @Column(name = "event_status")
    private String eventStatus;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Long getDoseEventId() {
        return doseEventId;
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

    public OffsetDateTime getInjectedAt() {
        return injectedAt;
    }

    public Double getDoseUnits() {
        return doseUnits;
    }

    public String getDetectionMethod() {
        return detectionMethod;
    }

    public Double getAngleDegrees() {
        return angleDegrees;
    }

    public Double getConfidencePercent() {
        return confidencePercent;
    }

    public String getEventStatus() {
        return eventStatus;
    }

    public String getNotes() {
        return notes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setDoseEventId(Long doseEventId) {
        this.doseEventId = doseEventId;
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

    public void setInjectedAt(OffsetDateTime injectedAt) {
        this.injectedAt = injectedAt;
    }

    public void setDoseUnits(Double doseUnits) {
        this.doseUnits = doseUnits;
    }

    public void setDetectionMethod(String detectionMethod) {
        this.detectionMethod = detectionMethod;
    }

    public void setAngleDegrees(Double angleDegrees) {
        this.angleDegrees = angleDegrees;
    }

    public void setConfidencePercent(Double confidencePercent) {
        this.confidencePercent = confidencePercent;
    }

    public void setEventStatus(String eventStatus) {
        this.eventStatus = eventStatus;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }
}
