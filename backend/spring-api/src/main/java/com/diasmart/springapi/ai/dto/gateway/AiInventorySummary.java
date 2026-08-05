package com.diasmart.springapi.ai.dto.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiInventorySummary(
    @JsonProperty("evidence_reference") String evidenceReference,
    @JsonProperty("latest_status") String latestStatus,
    @JsonProperty("latest_estimated_units") double latestEstimatedUnits,
    @JsonProperty("shortage_event_count") int shortageEventCount
) {}
