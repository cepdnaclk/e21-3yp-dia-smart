package com.diasmart.springapi.ai.dto.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiGlucoseSummary(
    @JsonProperty("evidence_reference") String evidenceReference,
    @JsonProperty("unit") String unit,
    @JsonProperty("reading_count") int readingCount,
    @JsonProperty("average") double average,
    @JsonProperty("minimum") double minimum,
    @JsonProperty("maximum") double maximum,
    @JsonProperty("high_reading_count") int highReadingCount,
    @JsonProperty("low_reading_count") int lowReadingCount
) {}
