package com.diasmart.springapi.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DoseDTO {

    private String deviceUid;

    private Integer insulinDoseUnits;

    private Double doseUnits;

    private String detectionMethod;

    private String injectedAt;

    private Double angleDegrees;

    private Double confidencePercent;

    private String eventStatus;

    private String notes;

    // =========================
    // GETTERS
    // =========================

    public String getDeviceUid() {
        return deviceUid;
    }

    public Integer getInsulinDoseUnits() {
        return insulinDoseUnits;
    }

    public Double getDoseUnits() {
        return doseUnits;
    }

    public String getDetectionMethod() {
        return detectionMethod;
    }

    public String getInjectedAt() {
        return injectedAt;
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

    // =========================
    // SETTERS
    // =========================

    public void setDeviceUid(String deviceUid) {
        this.deviceUid = deviceUid;
    }

    public void setInsulinDoseUnits(
            Integer insulinDoseUnits
    ) {
        this.insulinDoseUnits = insulinDoseUnits;
    }

    public void setDoseUnits(Double doseUnits) {
        this.doseUnits = doseUnits;
    }

    public void setDetectionMethod(
            String detectionMethod
    ) {
        this.detectionMethod = detectionMethod;
    }

    public void setInjectedAt(String injectedAt) {
        this.injectedAt = injectedAt;
    }

    public void setAngleDegrees(Double angleDegrees) {
        this.angleDegrees = angleDegrees;
    }

    public void setConfidencePercent(
            Double confidencePercent
    ) {
        this.confidencePercent = confidencePercent;
    }

    public void setEventStatus(String eventStatus) {
        this.eventStatus = eventStatus;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
