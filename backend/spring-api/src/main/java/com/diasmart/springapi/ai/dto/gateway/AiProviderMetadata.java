package com.diasmart.springapi.ai.dto.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiProviderMetadata(
    @JsonProperty("provider") String provider,
    @JsonProperty("model") String model,
    @JsonProperty("prompt_version") String promptVersion
) {}
