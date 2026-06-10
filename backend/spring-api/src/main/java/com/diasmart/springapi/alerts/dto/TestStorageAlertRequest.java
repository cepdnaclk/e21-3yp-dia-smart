package com.diasmart.springapi.alerts.dto;

public class TestStorageAlertRequest {

    private Long patientId;

    private Double temperatureC;

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Double getTemperatureC() {
        return temperatureC;
    }

    public void setTemperatureC(
            Double temperatureC
    ) {
        this.temperatureC = temperatureC;
    }
}