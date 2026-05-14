package com.diasmart.springapi.inventory.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory_readings")
public class InventoryReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_reading_id")
    private Long inventoryReadingId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "raw_event_id")
    private Long rawEventId;

    @Column(name = "measured_at")
    private OffsetDateTime measuredAt;

    @Column(name = "pen_present")
    private Boolean penPresent;

    @Column(name = "cartridge_present")
    private Boolean cartridgePresent;

    @Column(name = "weight_g")
    private Double weightG;

    @Column(name = "estimated_units_remaining")
    private Double estimatedUnitsRemaining;

    @Column(name = "estimated_remaining_percent")
    private Double estimatedRemainingPercent;

    @Column(name = "inventory_status")
    private String inventoryStatus;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    // =========================
    // GETTERS
    // =========================

    public Long getInventoryReadingId() {
        return inventoryReadingId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public Long getRawEventId() {
        return rawEventId;
    }

    public OffsetDateTime getMeasuredAt() {
        return measuredAt;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    // =========================
    // SETTERS
    // =========================

    public void setInventoryReadingId(
            Long inventoryReadingId
    ) {
        this.inventoryReadingId = inventoryReadingId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public void setRawEventId(Long rawEventId) {
        this.rawEventId = rawEventId;
    }

    public void setMeasuredAt(OffsetDateTime measuredAt) {
        this.measuredAt = measuredAt;
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

    public void setCreatedAt(
            OffsetDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }
}