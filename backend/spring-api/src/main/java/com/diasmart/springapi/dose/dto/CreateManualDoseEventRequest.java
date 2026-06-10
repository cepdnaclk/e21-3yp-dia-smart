package com.diasmart.springapi.dose.dto;

public class CreateManualDoseEventRequest {

    private Double doseUnits;

    private String injectedAt;

    private String notes;

    public Double getDoseUnits() {
        return doseUnits;
    }

    public void setDoseUnits(Double doseUnits) {
        this.doseUnits = doseUnits;
    }

    public String getInjectedAt() {
        return injectedAt;
    }

    public void setInjectedAt(String injectedAt) {
        this.injectedAt = injectedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}