package com.diasmart.springapi.inventory.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


// Maps inventory_readings table
@Entity
@Table(name = "inventory_readings")

@Getter
@Setter
public class InventoryReading {

    // Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inventoryReadingId;


    // Related patient
    @Column(name = "patient_id")
    private Long patientId;


    // Related device
    @Column(name = "device_id")
    private Long deviceId;


    // Raw telemetry event reference
    @Column(name = "raw_event_id")
    private Long rawEventId;


    // Measurement timestamp
    @Column(name = "measured_at")
    private OffsetDateTime measuredAt;


    // Pen detected or not
    @Column(name = "pen_present")
    private Boolean penPresent;


    // Cartridge detected or not
    @Column(name = "cartridge_present")
    private Boolean cartridgePresent;


    // Cartridge weight
    @Column(name = "weight_g")
    private Double weightG;


    // Estimated insulin units remaining
    @Column(name = "estimated_units_remaining")
    private Double estimatedUnitsRemaining;


    // Remaining percentage
    @Column(name = "estimated_remaining_percent")
    private Double estimatedRemainingPercent;


    // Inventory health status
    @Column(name = "inventory_status")
    private String inventoryStatus;


    // Additional notes
    @Column(name = "notes")
    private String notes;


    // Record creation timestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}