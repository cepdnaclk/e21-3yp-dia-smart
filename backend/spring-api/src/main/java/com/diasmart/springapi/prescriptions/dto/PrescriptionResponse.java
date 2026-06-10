package com.diasmart.springapi.prescriptions.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class PrescriptionResponse {

    private Long prescriptionId;

    private Long insulinProductId;

    private String prescriptionName;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean active;

    private String notes;

    private OffsetDateTime createdAt;

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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