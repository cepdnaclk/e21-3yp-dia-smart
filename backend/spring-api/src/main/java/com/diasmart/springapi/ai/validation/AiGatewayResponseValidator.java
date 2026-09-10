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

    public static final Set<String> VALID_EVIDENCE_CATEGORIES = Set.of(
            "glucose-summary",
            "adherence-summary",
            "storage-summary",
            "inventory-summary",
            "glucose-event",
            "administration-event",
            "storage-event",
            "inventory-event",
            "alert-event"
    );

    private static final Set<String> VALID_CONFIDENCE_LEVELS = Set.of(
            "HIGH",
            "MEDIUM",
            "LOW"
    );

    // Keywords or phrases indicating prohibited clinical instructions (diagnosis, prescription, dosage, treatment, causation)
    private static final List<Pattern> CLINICAL_SAFETY_PATTERNS = List.of(
            Pattern.compile("\\b(diagnose|diagnosis|diagnosed|diagnostic)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(prescribe|prescription|prescribed|start\\s+taking|stop\\s+taking|medication\\s+change)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(increase|decrease|adjust|change|modify|administer|inject|take)\\s+(your\\s+)?(insulin|dose|dosage|units)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(insulin\\s*-\\s*dosage|dosage\\s+recommendation|dose\\s+adjustment)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(treatment\\s+recommendation|treatment\\s+plan\\s+change|change\\s+your\\s+treatment)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(definitely\\s+caused\\s+by|proves\\s+that|conclusively\\s+demonstrates|direct\\s+cause\\s+of)\\b", Pattern.CASE_INSENSITIVE)
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
        if (!"mock".equalsIgnoreCase(response.providerMetadata().provider())) {
            throw new AiInvalidResponseException("AI response provider is not mock: " + response.providerMetadata().provider());
        }
        if (!request.promptVersion().equals(response.providerMetadata().promptVersion())) {
            throw new AiInvalidResponseException("AI response prompt version mismatch.");
        }

        // 4. Uncertainty check (must be non-empty)
        if (response.uncertainties() == null || response.uncertainties().isEmpty()) {
            throw new AiInvalidResponseException("AI response uncertainties section is empty or missing.");
        }
        for (String uncertainty : response.uncertainties()) {
            if (uncertainty == null || uncertainty.isBlank()) {
                throw new AiInvalidResponseException("AI response uncertainty entry cannot be empty.");
            }
            checkClinicalSafety(uncertainty);
        }

        // 5. Citation tracking: Ensure every citation exists in the request evidence.
        Set<String> validRefs = collectValidEvidenceReferences(request);

        if (response.observations() != null) {
            for (AiObservation obs : response.observations()) {
                validateObservation(obs, validRefs);
            }
        }

        if (response.correlations() != null) {
            for (AiCorrelation corr : response.correlations()) {
                validateCorrelation(corr, validRefs);
            }
        }

        // 6. Clinical Safety filter (Screen the generated text)
        checkClinicalSafety(response.summary());
        if (response.discussionPoints() != null) {
            for (String dp : response.discussionPoints()) {
                checkClinicalSafety(dp);
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

    private void validateObservation(AiObservation obs, Set<String> validRefs) {
        if (obs == null) {
            throw new AiInvalidResponseException("Observation entry cannot be null.");
        }
        if (obs.statement() == null || obs.statement().isBlank()) {
            throw new AiInvalidResponseException("Observation statement cannot be empty.");
        }
        validateCitations(obs.statement(), obs.evidenceReferences(), validRefs, 1);
        checkClinicalSafety(obs.statement());
    }

    private void validateCorrelation(AiCorrelation corr, Set<String> validRefs) {
        if (corr == null) {
            throw new AiInvalidResponseException("Correlation entry cannot be null.");
        }
        if (corr.statement() == null || corr.statement().isBlank()) {
            throw new AiInvalidResponseException("Correlation statement cannot be empty.");
        }
        validateConfidence(corr.confidence());
        // Correlations must cite at least two distinct valid evidence references
        validateCitations(corr.statement(), corr.evidenceReferences(), validRefs, 2);
        checkClinicalSafety(corr.statement());
    }

    private void validateConfidence(String confidence) {
        if (confidence == null || !VALID_CONFIDENCE_LEVELS.contains(confidence.trim().toUpperCase())) {
            throw new AiInvalidResponseException("Unsupported confidence level: " + confidence);
        }
    }

    private void validateCitations(String statement, List<String> citations, Set<String> validRefs, int minCount) {
        if (citations == null || citations.size() < minCount) {
            throw new AiInvalidResponseException("Statement requires at least " + minCount + " evidence citations: " + statement);
        }
        Set<String> uniqueCitations = new HashSet<>(citations);
        if (uniqueCitations.size() < minCount) {
            throw new AiInvalidResponseException("Statement requires at least " + minCount + " distinct evidence citations: " + statement);
        }
        for (String citation : citations) {
            validateCitationFormat(citation);
            if (!validRefs.contains(citation)) {
                throw new AiInvalidResponseException("Invalid or uncited evidence reference in response: " + citation);
            }
        }
    }

    private void validateCitationFormat(String citation) {
        if (citation == null || citation.isBlank() || !citation.contains(":")) {
            throw new AiInvalidResponseException("Invalid evidence reference format: " + citation);
        }
        String category = citation.substring(0, citation.indexOf(':'));
        if (!VALID_EVIDENCE_CATEGORIES.contains(category)) {
            throw new AiInvalidResponseException("Invalid evidence reference category: " + category);
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
