package com.diasmart.springapi.ai.dto.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiStorageSummary(
    @JsonProperty("evidence_reference") String evidenceReference,
    @JsonProperty("unit") String unit,
    @JsonProperty("reading_count") int readingCount,
    @JsonProperty("average_temperature") double averageTemperature,
    @JsonProperty("minimum_temperature") double minimumTemperature,
    @JsonProperty("maximum_temperature") double maximumTemperature,
    @JsonProperty("excursion_count") int excursionCount
) {}
