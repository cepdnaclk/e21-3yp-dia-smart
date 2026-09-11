package com.diasmart.springapi.ai.dto.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record AiSelectedEvent(
    @JsonProperty("evidence_reference") String evidenceReference,
    @JsonProperty("event_type") String eventType,
    @JsonProperty("recorded_at") OffsetDateTime recordedAt,
    @JsonProperty("value") Double value,
    @JsonProperty("unit") String unit,
    @JsonProperty("status") String status,
    @JsonProperty("description") String description
) {}
