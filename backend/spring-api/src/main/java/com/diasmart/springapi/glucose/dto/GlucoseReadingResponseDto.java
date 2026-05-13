package com.diasmart.springapi.glucose.dto;

// Lombok annotations for auto-generating constructor, builder, and getters(for all fields)
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// Used for timestamp with timezone support
import java.time.OffsetDateTime;

// Auto-generates getter methods fro all fields
@Getter

// Enables builder pattern object creation
@Builder

// Auto-generates constructor with all fields
@AllArgsConstructor

// DTO used to send glucose data to frontend safely
public class GlucoseReadingResponseDto {

    // Unique glucose reading ID
    private Long glucoseReadingId;

    // ID of the patient
    private Long patientId;

    // Glucose value in mg/dL
    private Double glucoseValueMgDl;

    // Time when glucose was measured
    private OffsetDateTime measuredAt;
}