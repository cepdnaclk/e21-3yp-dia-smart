package com.diasmart.springapi.ai.dto.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record AiAlertContext(
    @JsonProperty("evidence_reference") String evidenceReference,
    @JsonProperty("alert_type") String alertType,
    @JsonProperty("severity") String severity,
    @JsonProperty("status") String status,
    @JsonProperty("recorded_at") OffsetDateTime recordedAt
) {}
