package com.diasmart.springapi.ai.dto.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiAdherenceSummary(
    @JsonProperty("evidence_reference") String evidenceReference,
    @JsonProperty("scheduled_administrations") int scheduledAdministrations,
    @JsonProperty("recorded_administrations") int recordedAdministrations,
    @JsonProperty("delayed_administrations") int delayedAdministrations,
    @JsonProperty("missed_administrations") int missedAdministrations
) {}
