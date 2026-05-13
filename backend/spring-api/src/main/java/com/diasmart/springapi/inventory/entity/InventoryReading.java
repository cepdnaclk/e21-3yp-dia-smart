package com.diasmart.springapi.inventory.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


// Maps this class to inventory_readings table
@Entity

// Database table name
@Table(name = "inventory_readings")

@Getter
@Setter
public class InventoryReading {

    // Primary key
    @Id

    // Auto-generated ID
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    // Maps inventory_reading_id column
    @Column(name = "inventory_reading_id")
    private Long inventoryReadingId;


    // Patient ID
    @Column(name = "patient_id")
    private Long patientId;


    // Reading timestamp
    @Column(name = "measured_at")
    private OffsetDateTime measuredAt;


    // Measured cartridge weight
    @Column(name = "weight_g")
    private Double weightG;


    // Estimated remaining insulin units
    @Column(name = "estimated_units_remaining")
    private Double estimatedUnitsRemaining;


    // Remaining insulin percentage
    @Column(name = "estimated_remaining_percent")
    private Double estimatedRemainingPercent;


    // Inventory health status
    @Column(name = "inventory_status")
    private String inventoryStatus;
}