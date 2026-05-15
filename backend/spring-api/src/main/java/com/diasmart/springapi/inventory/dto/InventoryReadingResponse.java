package com.diasmart.springapi.inventory.dto;

import java.time.OffsetDateTime;

public class InventoryReadingResponse {

    private Long inventoryReadingId;

    private Boolean penPresent;

    private Boolean cartridgePresent;

    private Double weightG;

    private Double estimatedUnitsRemaining;

    private Double estimatedRemainingPercent;

    private String inventoryStatus;

    private String notes;

    private OffsetDateTime measuredAt;

    private OffsetDateTime createdAt;

    public Long getInventoryReadingId() {
        return inventoryReadingId;
    }

    public void setInventoryReadingId(Long inventoryReadingId) {
        this.inventoryReadingId = inventoryReadingId;
    }

    public Boolean getPenPresent() {
        return penPresent;
    }

    public void setPenPresent(Boolean penPresent) {
        this.penPresent = penPresent;
    }

    public Boolean getCartridgePresent() {
        return cartridgePresent;
    }

    public void setCartridgePresent(Boolean cartridgePresent) {
        this.cartridgePresent = cartridgePresent;
    }

    public Double getWeightG() {
        return weightG;
    }

    public void setWeightG(Double weightG) {
        this.weightG = weightG;
    }

    public Double getEstimatedUnitsRemaining() {
        return estimatedUnitsRemaining;
    }

    public void setEstimatedUnitsRemaining(
            Double estimatedUnitsRemaining
    ) {
        this.estimatedUnitsRemaining =
                estimatedUnitsRemaining;
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

    public String getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(
            String inventoryStatus
    ) {
        this.inventoryStatus = inventoryStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public OffsetDateTime getMeasuredAt() {
        return measuredAt;
    }

    public void setMeasuredAt(
            OffsetDateTime measuredAt
    ) {
        this.measuredAt = measuredAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            OffsetDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }
}