package com.diasmart.springapi.ai.dto.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiCorrelation(
    @JsonProperty("statement") String statement,
    @JsonProperty("confidence") String confidence,
    @JsonProperty("evidence_references") List<String> evidenceReferences
) {}
