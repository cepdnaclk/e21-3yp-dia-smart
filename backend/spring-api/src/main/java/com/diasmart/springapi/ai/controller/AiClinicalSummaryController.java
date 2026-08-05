package com.diasmart.springapi.ai.controller;

import com.diasmart.springapi.ai.client.AiGatewayClient;
import com.diasmart.springapi.ai.config.AiProperties;
import com.diasmart.springapi.ai.dto.api.AiClinicalSummaryApiResponse;
import com.diasmart.springapi.ai.dto.gateway.AiClinicalSummaryGatewayRequest;
import com.diasmart.springapi.ai.dto.gateway.AiClinicalSummaryGatewayResponse;
import com.diasmart.springapi.ai.exception.AiDisabledException;
import com.diasmart.springapi.ai.service.PatientAiContextService;
import com.diasmart.springapi.ai.validation.AiGatewayResponseValidator;
import com.diasmart.springapi.audit.service.AuditService;
import com.diasmart.springapi.common.exceptions.ApiException;
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

@RestController
@RequestMapping("/api/v1/patients")
public class AiClinicalSummaryController {

    private static final Logger log = LoggerFactory.getLogger(AiClinicalSummaryController.class);

    private final PatientAiContextService patientAiContextService;
    private final AiGatewayClient aiGatewayClient;
    private final AiGatewayResponseValidator aiGatewayResponseValidator;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final AiProperties aiProperties;

    public AiClinicalSummaryController(
            PatientAiContextService patientAiContextService,
            AiGatewayClient aiGatewayClient,
            AiGatewayResponseValidator aiGatewayResponseValidator,
            AuditService auditService,
            CurrentUserService currentUserService,
            AiProperties aiProperties
    ) {
        this.patientAiContextService = patientAiContextService;
        this.aiGatewayClient = aiGatewayClient;
        this.aiGatewayResponseValidator = aiGatewayResponseValidator;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
        this.aiProperties = aiProperties;
    }

    @GetMapping("/{patientId}/ai-summary")
    public ResponseEntity<AiClinicalSummaryApiResponse> getPatientAiSummary(
            @PathVariable Long patientId,
            @RequestParam("from") String fromStr,
            @RequestParam("to") String toStr
    ) {
        long startTime = System.currentTimeMillis();

        // 1. Feature Flag Check
        if (!aiProperties.isEnabled()) {
            log.warn("AI summary requested for patient {} but AI integration is disabled.", patientId);
            throw new AiDisabledException();
        }

        log.info("Starting AI summary generation lifecycle for patient {}. Params: from={}, to={}", patientId, fromStr, toStr);

        // 2. Parse and Validate Period
        OffsetDateTime from;
        OffsetDateTime to;
        try {
            from = OffsetDateTime.parse(fromStr.trim());
            to = OffsetDateTime.parse(toStr.trim());
        } catch (Exception e) {
            log.error("Failed to parse requested range: from={}, to={}. Error: {}", fromStr, toStr, e.getMessage());
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

        // 3. Build Pseudonymous Context Payload
        AiClinicalSummaryGatewayRequest gatewayRequest = patientAiContextService.buildGatewayRequest(patientId, from, to);

        // 4. Request microservice summary
        log.info("Sending clinical summary request to AI gateway. Request ID: {}, Pseudonym: {}", gatewayRequest.requestId(), gatewayRequest.patientReference());
        AiClinicalSummaryGatewayResponse gatewayResponse = aiGatewayClient.requestClinicalSummary(gatewayRequest);

        // 5. Validate microservice response
        log.info("Received AI gateway response. Validating integrity...");
        aiGatewayResponseValidator.validateResponse(gatewayRequest, gatewayResponse);

        // 6. Record Audit Logs
        Long userId = null;
        try {
            userId = currentUserService.getCurrentUserId();
        } catch (Exception e) {
            log.debug("Unauthenticated call or missing SecurityContext: {}", e.getMessage());
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("requestId", gatewayRequest.requestId());
        details.put("from", from);
        details.put("to", to);
        details.put("pseudonymousRef", gatewayRequest.patientReference());
        details.put("provider", gatewayResponse.providerMetadata().provider());
        details.put("model", gatewayResponse.providerMetadata().model());

        auditService.record(
                userId,
                patientId,
                "AI_CLINICAL_SUMMARY_GENERATED",
                "PATIENT",
                patientId,
                null,
                details
        );

        long duration = System.currentTimeMillis() - startTime;
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
    }
}
