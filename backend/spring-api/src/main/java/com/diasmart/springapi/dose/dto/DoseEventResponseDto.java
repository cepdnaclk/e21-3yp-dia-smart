package com.diasmart.springapi.dose.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;


// Response DTO sent to frontend
@Getter
@Builder
@AllArgsConstructor
public class DoseEventResponseDto {

    // Dose event ID
    private Long doseEventId;

    // Patient ID
    private Long patientId;

    // Insulin dose amount
    private Double doseUnits;

    // Time of injection
    private OffsetDateTime injectedAt;

    // Detection method used
    private String detectionMethod;

    // Event status
    private String eventStatus;
}