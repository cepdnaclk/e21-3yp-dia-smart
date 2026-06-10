package com.diasmart.springapi.dose.dto;

import java.time.OffsetDateTime;

public class DoseEventResponse {

    private Long doseEventId;

    private Double doseUnits;

    private OffsetDateTime injectedAt;

    private String detectionMethod;

    private String eventStatus;

    private String notes;

    private OffsetDateTime createdAt;

    public Long getDoseEventId() {
        return doseEventId;
    }

    public void setDoseEventId(Long doseEventId) {
        this.doseEventId = doseEventId;
    }

    public Double getDoseUnits() {
        return doseUnits;
    }

    public void setDoseUnits(Double doseUnits) {
        this.doseUnits = doseUnits;
    }

    public OffsetDateTime getInjectedAt() {
        return injectedAt;
    }

    public void setInjectedAt(OffsetDateTime injectedAt) {
        this.injectedAt = injectedAt;
    }

    public String getDetectionMethod() {
        return detectionMethod;
    }

    public void setDetectionMethod(String detectionMethod) {
        this.detectionMethod = detectionMethod;
    }

    public String getEventStatus() {
        return eventStatus;
    }

    public void setEventStatus(String eventStatus) {
        this.eventStatus = eventStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}