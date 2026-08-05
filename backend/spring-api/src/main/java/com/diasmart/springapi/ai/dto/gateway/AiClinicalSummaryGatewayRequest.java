package com.diasmart.springapi.ai.dto.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record AiClinicalSummaryGatewayRequest(
    @JsonProperty("request_id") UUID requestId,
    @JsonProperty("request_type") String requestType,
    @JsonProperty("prompt_version") String promptVersion,
    @JsonProperty("patient_reference") String patientReference,
    @JsonProperty("requested_period") AiRequestedPeriod requestedPeriod,
    @JsonProperty("glucose_summary") AiGlucoseSummary glucoseSummary,
    @JsonProperty("adherence_summary") AiAdherenceSummary adherenceSummary,
    @JsonProperty("storage_summary") AiStorageSummary storageSummary,
    @JsonProperty("inventory_summary") AiInventorySummary inventorySummary,
    @JsonProperty("relevant_alerts") List<AiAlertContext> relevantAlerts,
    @JsonProperty("selected_events") List<AiSelectedEvent> selectedEvents
) {}
