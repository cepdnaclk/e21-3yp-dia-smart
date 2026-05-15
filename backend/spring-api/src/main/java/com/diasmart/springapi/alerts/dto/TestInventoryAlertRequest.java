package com.diasmart.springapi.alerts.dto;

public class TestInventoryAlertRequest {

    private Long patientId;

    private Double estimatedRemainingPercent;

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Double getEstimatedRemainingPercent() {
        return estimatedRemainingPercent;
    }

    public void setEstimatedRemainingPercent(
            Double estimatedRemainingPercent
    ) {
        this.estimatedRemainingPercent =
                estimatedRemainingPercent;
    }
}