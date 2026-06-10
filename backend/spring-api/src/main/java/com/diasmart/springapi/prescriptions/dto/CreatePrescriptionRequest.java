package com.diasmart.springapi.prescriptions.dto;
import jakarta.validation.constraints.NotBlank;

public class CreatePrescriptionRequest {

    private Long insulinProductId;

    @NotBlank(message = "Prescription name is required")
    private String prescriptionName;

    private String startDate;

    private String endDate;

    private String notes;

    public Long getInsulinProductId() {
        return insulinProductId;
    }

    public void setInsulinProductId(Long insulinProductId) {
        this.insulinProductId = insulinProductId;
    }

    public String getPrescriptionName() {
        return prescriptionName;
    }

    public void setPrescriptionName(String prescriptionName) {
        this.prescriptionName = prescriptionName;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}