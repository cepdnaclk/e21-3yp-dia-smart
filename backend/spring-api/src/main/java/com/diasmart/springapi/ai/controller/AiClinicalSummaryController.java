package com.diasmart.springapi.ai.controller;

import com.diasmart.springapi.ai.client.AiGatewayClient;
import com.diasmart.springapi.ai.config.AiProperties;
import com.diasmart.springapi.ai.dto.api.AiClinicalSummaryApiResponse;
import com.diasmart.springapi.ai.dto.gateway.AiClinicalSummaryGatewayRequest;
import com.diasmart.springapi.ai.dto.gateway.AiClinicalSummaryGatewayResponse;
import com.diasmart.springapi.ai.exception.AiConfigurationException;
import com.diasmart.springapi.ai.exception.AiDisabledException;
import com.diasmart.springapi.ai.service.PatientAiContextService;
import com.diasmart.springapi.ai.validation.AiGatewayResponseValidator;
import com.diasmart.springapi.audit.service.AuditService;
import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;
import com.diasmart.springapi.shared.security.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
public class AiClinicalSummaryController {

    private static final Logger log = LoggerFactory.getLogger(AiClinicalSummaryController.class);

    private final PatientAiContextService patientAiContextService;
    private final AiGatewayClient aiGatewayClient;
    private final AiGatewayResponseValidator aiGatewayResponseValidator;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;
    private final AiProperties aiProperties;

    public AiClinicalSummaryController(
            PatientAiContextService patientAiContextService,
            AiGatewayClient aiGatewayClient,
            AiGatewayResponseValidator aiGatewayResponseValidator,
            AuditService auditService,
            CurrentUserService currentUserService,
            AuthorizationService authorizationService,
            AiProperties aiProperties
    ) {
        this.patientAiContextService = patientAiContextService;
        this.aiGatewayClient = aiGatewayClient;
        this.aiGatewayResponseValidator = aiGatewayResponseValidator;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.aiProperties = aiProperties;
    }

    @GetMapping("/{patientId}/ai-summary")
    public ResponseEntity<AiClinicalSummaryApiResponse> getPatientAiSummary(
            @PathVariable Long patientId,
            @RequestParam("from") String fromStr,
            @RequestParam("to") String toStr
    ) {
        long startTime = System.currentTimeMillis();
        UUID requestId = null;
        OffsetDateTime from = null;
        OffsetDateTime to = null;

        // 1. Authorize patient access (caller must be authenticated & authorized before proceeding)
        authorizationService.authorize(Permission.READ_PATIENT_READINGS, patientId);

        // 2. Feature Flag Check
        if (!aiProperties.isEnabled()) {
            log.warn("AI summary requested for patient {} but AI integration is disabled.", patientId);
            throw new AiDisabledException();
        }

        // 3. Configuration Integrity Check
        if (aiProperties.getGatewayUrl() == null || aiProperties.getGatewayUrl().isBlank()
                || aiProperties.getInternalServiceToken() == null || aiProperties.getInternalServiceToken().isBlank()) {
            log.error("AI service configuration is invalid or incomplete.");
            throw new AiConfigurationException("AI service configuration is invalid or incomplete.");
        }

        // 4. Parse and Validate Period
        try {
            from = OffsetDateTime.parse(fromStr.trim());
            to = OffsetDateTime.parse(toStr.trim());
        } catch (Exception e) {
            log.error("Failed to parse requested range: from={}, to={}", fromStr, toStr);
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "Request parameters 'from' and 'to' must be timezone-aware ISO-8601 strings.");
        }

        if (!from.isBefore(to)) {
            log.warn("Chronology validation failed: 'from' ({}) must be before 'to' ({}).", from, to);
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "Start timestamp must be strictly before end timestamp.");
        }

        long days = ChronoUnit.DAYS.between(from, to);
        if (days > aiProperties.getMaxDateRangeDays()) {
            log.warn("Date range validation failed: requested {} days, limit is {} days.", days, aiProperties.getMaxDateRangeDays());
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "Requested period duration exceeds the maximum limit of " + aiProperties.getMaxDateRangeDays() + " days.");
        }

        try {
            // 5. Retrieve patient context
            AiClinicalSummaryGatewayRequest gatewayRequest = patientAiContextService.buildGatewayRequest(patientId, from, to);
            requestId = gatewayRequest.requestId();

            // 6. Request microservice summary (sanitized logging without patient reference or payload)
            log.info("Sending clinical summary request to AI gateway. Request ID: {}", requestId);
            AiClinicalSummaryGatewayResponse gatewayResponse = aiGatewayClient.requestClinicalSummary(gatewayRequest);

            // 7. Validate microservice response
            log.info("Received AI gateway response. Validating integrity...");
            aiGatewayResponseValidator.validateResponse(gatewayRequest, gatewayResponse);

            long duration = System.currentTimeMillis() - startTime;
            recordAuditSuccess(patientId, requestId, from, to, gatewayRequest, gatewayResponse, duration);
            log.info("Successfully generated and verified AI clinical summary for patient {} in {} ms.", patientId, duration);

            AiClinicalSummaryApiResponse apiResponse = new AiClinicalSummaryApiResponse(
                    gatewayResponse.requestId(),
                    from,
                    to,
                    OffsetDateTime.now(),
                    gatewayResponse.summary(),
                    gatewayResponse.observations(),
                    gatewayResponse.correlations(),
                    gatewayResponse.uncertainties(),
                    gatewayResponse.discussionPoints(),
                    gatewayResponse.safetyNotice(),
                    gatewayResponse.providerMetadata()
            );

            return ResponseEntity.ok(apiResponse);
        } catch (ApiException ex) {
            long duration = System.currentTimeMillis() - startTime;
            recordAuditFailure(patientId, requestId, from, to, ex.getErrorCode(), duration);
            throw ex;
        }
    }

    private void recordAuditSuccess(
            Long patientId,
            UUID requestId,
            OffsetDateTime from,
            OffsetDateTime to,
            AiClinicalSummaryGatewayRequest request,
            AiClinicalSummaryGatewayResponse response,
            long durationMs
    ) {
        Long userId = resolveUserId();
        int sectionCount = 0;
        if (request.glucoseSummary() != null) sectionCount++;
        if (request.adherenceSummary() != null) sectionCount++;
        if (request.storageSummary() != null) sectionCount++;
        if (request.inventorySummary() != null) sectionCount++;

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("requestId", requestId);
        details.put("from", from);
        details.put("to", to);
        details.put("actionType", "CLINICAL_SUMMARY");
        details.put("promptVersion", request.promptVersion());
        details.put("provider", response.providerMetadata() != null ? response.providerMetadata().provider() : "mock");
        details.put("model", response.providerMetadata() != null ? response.providerMetadata().model() : "mock-model");
        details.put("status", "SUCCESS");
        details.put("durationMs", durationMs);
        details.put("contextSectionCount", sectionCount);
        details.put("selectedEventCount", request.selectedEvents() != null ? request.selectedEvents().size() : 0);
        details.put("alertCount", request.relevantAlerts() != null ? request.relevantAlerts().size() : 0);

        auditService.record(
                userId,
                patientId,
                "AI_CLINICAL_SUMMARY_GENERATED",
                "PATIENT",
                patientId,
                null,
                details
        );
    }

    private void recordAuditFailure(
            Long patientId,
            UUID requestId,
            OffsetDateTime from,
            OffsetDateTime to,
            String errorCode,
            long durationMs
    ) {
        Long userId = resolveUserId();

        Map<String, Object> details = new LinkedHashMap<>();
        if (requestId != null) {
            details.put("requestId", requestId);
        }
        if (from != null) {
            details.put("from", from);
        }
        if (to != null) {
            details.put("to", to);
        }
        details.put("actionType", "CLINICAL_SUMMARY");
        details.put("status", "FAILURE");
        details.put("errorCode", errorCode != null ? errorCode : "UNKNOWN");
        details.put("durationMs", durationMs);

        auditService.record(
                userId,
                patientId,
                "AI_CLINICAL_SUMMARY_FAILED",
                "PATIENT",
                patientId,
                null,
                details
        );
    }

    private Long resolveUserId() {
        try {
            return currentUserService.getCurrentUserId();
        } catch (Exception e) {
            log.debug("Unauthenticated call or missing SecurityContext: {}", e.getMessage());
            return null;
        }
    }
}
