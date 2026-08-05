package com.diasmart.springapi.ai.validation;

import com.diasmart.springapi.ai.dto.gateway.*;
import com.diasmart.springapi.ai.exception.AiInvalidResponseException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class AiGatewayResponseValidator {

    public static final String APPROVED_SAFETY_NOTICE =
            "This AI-generated information is intended for review and does not provide a diagnosis, prescription, insulin-dosage recommendation, or treatment recommendation.";

    // Keywords or phrases indicating prohibited clinical instructions (diagnosis, prescription, treatment)
    private static final List<Pattern> CLINICAL_SAFETY_PATTERNS = List.of(
            Pattern.compile("\\b(diagnose|diagnosis|diagnosed)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(prescribe|prescription|prescribed)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(increase|decrease|adjust|change)\\s+(your\\s+)?(insulin|dose|dosage)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(insulin\\s*-\\s*dosage|dosage\\s+recommendation|treatment\\s+recommendation)\\b", Pattern.CASE_INSENSITIVE)
    );

    public void validateResponse(AiClinicalSummaryGatewayRequest request, AiClinicalSummaryGatewayResponse response) {
        if (response == null) {
            throw new AiInvalidResponseException("AI microservice response is null.");
        }

        // 1. Request ID matching
        if (response.requestId() == null || !response.requestId().equals(request.requestId())) {
            throw new AiInvalidResponseException("AI response request ID mismatch. Expected: "
                    + request.requestId() + ", Received: " + response.requestId());
        }

        // 2. Safety notice check
        if (!APPROVED_SAFETY_NOTICE.equals(response.safetyNotice())) {
            throw new AiInvalidResponseException("AI response safety notice is missing or modified.");
        }

        // 3. Prompt version and provider metadata validation
        if (response.providerMetadata() == null) {
            throw new AiInvalidResponseException("AI response provider metadata is missing.");
        }
        if (!request.promptVersion().equals(response.providerMetadata().promptVersion())) {
            throw new AiInvalidResponseException("AI response prompt version mismatch.");
        }

        // 4. Citation tracking: Ensure every citation exists in the request evidence.
        Set<String> validRefs = collectValidEvidenceReferences(request);

        if (response.observations() != null) {
            for (AiObservation obs : response.observations()) {
                validateCitations(obs.statement(), obs.evidenceReferences(), validRefs);
            }
        }

        if (response.correlations() != null) {
            for (AiCorrelation corr : response.correlations()) {
                validateCitations(corr.statement(), corr.evidenceReferences(), validRefs);
            }
        }

        // 5. Clinical Safety filter (Screen the generated text)
        checkClinicalSafety(response.summary());
        if (response.observations() != null) {
            for (AiObservation obs : response.observations()) {
                checkClinicalSafety(obs.statement());
            }
        }
        if (response.correlations() != null) {
            for (AiCorrelation corr : response.correlations()) {
                checkClinicalSafety(corr.statement());
            }
        }
    }

    private Set<String> collectValidEvidenceReferences(AiClinicalSummaryGatewayRequest request) {
        Set<String> refs = new HashSet<>();
        if (request.glucoseSummary() != null) {
            refs.add(request.glucoseSummary().evidenceReference());
        }
        if (request.adherenceSummary() != null) {
            refs.add(request.adherenceSummary().evidenceReference());
        }
        if (request.storageSummary() != null) {
            refs.add(request.storageSummary().evidenceReference());
        }
        if (request.inventorySummary() != null) {
            refs.add(request.inventorySummary().evidenceReference());
        }
        if (request.relevantAlerts() != null) {
            for (AiAlertContext alert : request.relevantAlerts()) {
                refs.add(alert.evidenceReference());
            }
        }
        if (request.selectedEvents() != null) {
            for (AiSelectedEvent ev : request.selectedEvents()) {
                refs.add(ev.evidenceReference());
            }
        }
        return refs;
    }

    private void validateCitations(String statement, List<String> citations, Set<String> validRefs) {
        if (citations == null || citations.isEmpty()) {
            throw new AiInvalidResponseException("Observation or correlation is missing citations: " + statement);
        }
        for (String citation : citations) {
            if (!validRefs.contains(citation)) {
                throw new AiInvalidResponseException("Invalid or uncited evidence reference in response: " + citation);
            }
        }
    }

    private void checkClinicalSafety(String text) {
        if (text == null) {
            return;
        }
        for (Pattern pattern : CLINICAL_SAFETY_PATTERNS) {
            if (pattern.matcher(text).find()) {
                throw new AiInvalidResponseException("AI summary response failed clinical safety filter due to prohibited diagnosis/prescription statement.");
            }
        }
    }
}
