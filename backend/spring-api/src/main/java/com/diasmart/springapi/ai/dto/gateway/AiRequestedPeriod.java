package com.diasmart.springapi.ai.dto.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record AiRequestedPeriod(
    @JsonProperty("from") OffsetDateTime from,
    @JsonProperty("to") OffsetDateTime to
) {}
