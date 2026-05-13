package com.diasmart.springapi.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;


// DTO sent to frontend
@Getter
@Builder
@AllArgsConstructor
public class StorageReadingResponseDto {

    // Storage reading ID
    private Long storageReadingId;

    // Patient ID
    private Long patientId;

    // Refrigerator temperature
    private Double temperatureC;

    // Refrigerator humidity
    private Double humidityPercent;

    // Door state
    private String doorState;

    // Door open duration
    private Integer doorOpenDurationSeconds;

    // Temperature safety status
    private String temperatureStatus;

    // Reading timestamp
    private OffsetDateTime measuredAt;
}