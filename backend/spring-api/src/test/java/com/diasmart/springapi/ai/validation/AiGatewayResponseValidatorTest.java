package com.diasmart.springapi.ai.validation;

import com.diasmart.springapi.ai.dto.gateway.*;
import com.diasmart.springapi.ai.exception.AiInvalidResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AiGatewayResponseValidatorTest {

    private AiGatewayResponseValidator validator;
    private AiClinicalSummaryGatewayRequest validRequest;
    private AiClinicalSummaryGatewayResponse validResponse;

    private final UUID requestId = UUID.randomUUID();
    private final String promptVersion = "clinical-summary-v1";
    private final String pseudonymousRef = "patient-ref-123";

    @BeforeEach
    void setUp() {
        validator = new AiGatewayResponseValidator();

        AiRequestedPeriod period = new AiRequestedPeriod(OffsetDateTime.now().minusDays(7), OffsetDateTime.now());

        AiGlucoseSummary glucoseSummary = new AiGlucoseSummary(
                "glucose_summary:selected-period",
                "mg/dL",
                10,
                110.0,
                75.0,
                160.0,
                2,
                1
        );

        AiAdherenceSummary adherenceSummary = new AiAdherenceSummary(
                "adherence_summary:selected-period",
                7,
                6,
                1,
                1
        );

        AiAlertContext alert = new AiAlertContext(
                "alert:id-45",
                "TEMP_HIGH",
                "CRITICAL",
                "OPEN",
                OffsetDateTime.now()
        );

        AiSelectedEvent event = new AiSelectedEvent(
                "glucose-reading:id-1",
                "GLUCOSE_HIGH",
                OffsetDateTime.now(),
                185.0,
                "mg/dL",
                "HIGH",
                "Glucose value: 185.0 mg/dL"
        );

        validRequest = new AiClinicalSummaryGatewayRequest(
                requestId,
                "CLINICAL_SUMMARY",
                promptVersion,
                pseudonymousRef,
                period,
                glucoseSummary,
                adherenceSummary,
                null,
                null,
                List.of(alert),
                List.of(event)
        );

        AiObservation obs = new AiObservation(
                "The patient had a high glucose reading of 185.0 mg/dL.",
                List.of("glucose-reading:id-1")
        );

        AiCorrelation corr = new AiCorrelation(
                "High storage temperature correlates with lower adherence rate.",
                "moderate",
                List.of("adherence_summary:selected-period", "alert:id-45")
        );

        AiProviderMetadata metadata = new AiProviderMetadata(
                "mock",
                "mock-model",
                promptVersion
        );

        validResponse = new AiClinicalSummaryGatewayResponse(
                requestId,
                "Clinical summary overview: patient shows good overall progress.",
                List.of(obs),
                List.of(corr),
                List.of("Uncertainty statement details."),
                List.of("Discussion point 1"),
                AiGatewayResponseValidator.APPROVED_SAFETY_NOTICE,
                metadata
        );
    }

    @Test
    void shouldPassValidResponse() {
        assertDoesNotThrow(() -> validator.validateResponse(validRequest, validResponse));
    }

    @Test
    void shouldThrowWhenResponseIsNull() {
        assertThrows(AiInvalidResponseException.class, () -> validator.validateResponse(validRequest, null));
    }

    @Test
    void shouldThrowWhenRequestIdMismatches() {
        AiClinicalSummaryGatewayResponse badResponse = new AiClinicalSummaryGatewayResponse(
                UUID.randomUUID(),
                validResponse.summary(),
                validResponse.observations(),
                validResponse.correlations(),
                validResponse.uncertainties(),
                validResponse.discussionPoints(),
                validResponse.safetyNotice(),
                validResponse.providerMetadata()
        );
        assertThrows(AiInvalidResponseException.class, () -> validator.validateResponse(validRequest, badResponse));
    }

    @Test
    void shouldThrowWhenSafetyNoticeModified() {
        AiClinicalSummaryGatewayResponse badResponse = new AiClinicalSummaryGatewayResponse(
                requestId,
                validResponse.summary(),
                validResponse.observations(),
                validResponse.correlations(),
                validResponse.uncertainties(),
                validResponse.discussionPoints(),
                "Modified Safety Disclaimer",
                validResponse.providerMetadata()
        );
        assertThrows(AiInvalidResponseException.class, () -> validator.validateResponse(validRequest, badResponse));
    }

    @Test
    void shouldThrowWhenUncitedEvidenceReferenced() {
        AiObservation badObs = new AiObservation(
                "Statement with uncited evidence",
                List.of("glucose-reading:id-999") // 999 does not exist in request!
        );
        AiClinicalSummaryGatewayResponse badResponse = new AiClinicalSummaryGatewayResponse(
                requestId,
                validResponse.summary(),
                List.of(badObs),
                validResponse.correlations(),
                validResponse.uncertainties(),
                validResponse.discussionPoints(),
                validResponse.safetyNotice(),
                validResponse.providerMetadata()
        );
        assertThrows(AiInvalidResponseException.class, () -> validator.validateResponse(validRequest, badResponse));
    }

    @Test
    void shouldThrowWhenCitationsAreEmpty() {
        AiObservation badObs = new AiObservation(
                "Statement with no citations",
                Collections.emptyList()
        );
        AiClinicalSummaryGatewayResponse badResponse = new AiClinicalSummaryGatewayResponse(
                requestId,
                validResponse.summary(),
                List.of(badObs),
                validResponse.correlations(),
                validResponse.uncertainties(),
                validResponse.discussionPoints(),
                validResponse.safetyNotice(),
                validResponse.providerMetadata()
        );
        assertThrows(AiInvalidResponseException.class, () -> validator.validateResponse(validRequest, badResponse));
    }

    @Test
    void shouldThrowWhenClinicalSafetyFilterTriggered() {
        AiClinicalSummaryGatewayResponse badResponse = new AiClinicalSummaryGatewayResponse(
                requestId,
                "Please increase your insulin dose by 2 units.", // Prohibited text!
                validResponse.observations(),
                validResponse.correlations(),
                validResponse.uncertainties(),
                validResponse.discussionPoints(),
                validResponse.safetyNotice(),
                validResponse.providerMetadata()
        );
        assertThrows(AiInvalidResponseException.class, () -> validator.validateResponse(validRequest, badResponse));
    }
}
