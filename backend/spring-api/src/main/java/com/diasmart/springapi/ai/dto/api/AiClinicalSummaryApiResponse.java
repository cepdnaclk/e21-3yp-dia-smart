package com.diasmart.springapi.ai.dto.api;

import com.diasmart.springapi.ai.dto.gateway.AiCorrelation;
import com.diasmart.springapi.ai.dto.gateway.AiObservation;
import com.diasmart.springapi.ai.dto.gateway.AiProviderMetadata;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AiClinicalSummaryApiResponse(
    UUID requestId,
    OffsetDateTime periodFrom,
    OffsetDateTime periodTo,
    OffsetDateTime generatedAt,
    String summary,
    List<AiObservation> observations,
    List<AiCorrelation> correlations,
    List<String> uncertainties,
    List<String> discussionPoints,
    String safetyNotice,
    AiProviderMetadata providerMetadata
) {}
