package com.diasmart.springapi.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;


// DTO sent to frontend
@Getter
@Builder
@AllArgsConstructor
public class InventoryReadingResponseDto {

    // Inventory reading ID
    private Long inventoryReadingId;

    // Patient ID
    private Long patientId;

    // Weight in grams
    private Double weightG;

    // Estimated remaining insulin units
    private Double estimatedUnitsRemaining;

    // Remaining percentage
    private Double estimatedRemainingPercent;

    // Inventory status
    private String inventoryStatus;

    // Reading timestamp
    private OffsetDateTime measuredAt;
}