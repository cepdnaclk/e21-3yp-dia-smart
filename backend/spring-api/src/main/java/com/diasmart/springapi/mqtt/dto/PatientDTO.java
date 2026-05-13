package com.diasmart.springapi.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PatientDTO {

    private Long patientId;

    // =========================
    // GETTER
    // =========================

    public Long getPatientId() {
        return patientId;
    }

    // =========================
    // SETTER
    // =========================

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }
}
