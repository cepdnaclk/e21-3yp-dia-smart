package com.diasmart.springapi.ai.dto.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record AiClinicalSummaryGatewayResponse(
    @JsonProperty("request_id") UUID requestId,
    @JsonProperty("summary") String summary,
    @JsonProperty("observations") List<AiObservation> observations,
    @JsonProperty("correlations") List<AiCorrelation> correlations,
    @JsonProperty("uncertainties") List<String> uncertainties,
    @JsonProperty("discussion_points") List<String> discussionPoints,
    @JsonProperty("safety_notice") String safetyNotice,
    @JsonProperty("provider_metadata") AiProviderMetadata providerMetadata
) {}
