package com.diasmart.springapi.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryTelemetryDTO {

    private String deviceUid;

    private Boolean penPresent;

    private Boolean cartridgePresent;

    private Double weightG;

    private Double estimatedUnitsRemaining;

    private Double estimatedRemainingPercent;

    private String inventoryStatus;

    private String notes;

    // =========================
    // GETTERS
    // =========================

    public String getDeviceUid() {
        return deviceUid;
    }

    public Boolean getPenPresent() {
        return penPresent;
    }

    public Boolean getCartridgePresent() {
        return cartridgePresent;
    }

    public Double getWeightG() {
        return weightG;
    }

    public Double getEstimatedUnitsRemaining() {
        return estimatedUnitsRemaining;
    }

    public Double getEstimatedRemainingPercent() {
        return estimatedRemainingPercent;
    }

    public String getInventoryStatus() {
        return inventoryStatus;
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

    public void setPenPresent(Boolean penPresent) {
        this.penPresent = penPresent;
    }

    public void setCartridgePresent(
            Boolean cartridgePresent
    ) {
        this.cartridgePresent = cartridgePresent;
    }

    public void setWeightG(Double weightG) {
        this.weightG = weightG;
    }

    public void setEstimatedUnitsRemaining(
            Double estimatedUnitsRemaining
    ) {
        this.estimatedUnitsRemaining =
                estimatedUnitsRemaining;
    }

    public void setEstimatedRemainingPercent(
            Double estimatedRemainingPercent
    ) {
        this.estimatedRemainingPercent =
                estimatedRemainingPercent;
    }

    public void setInventoryStatus(
            String inventoryStatus
    ) {
        this.inventoryStatus = inventoryStatus;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
