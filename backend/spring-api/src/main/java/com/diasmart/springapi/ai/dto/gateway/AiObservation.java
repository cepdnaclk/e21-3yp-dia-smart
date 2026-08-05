package com.diasmart.springapi.ai.dto.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiObservation(
    @JsonProperty("statement") String statement,
    @JsonProperty("evidence_references") List<String> evidenceReferences
) {}
