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
    private final String pseudonymousRef = "patient-ref-12345678";

    @BeforeEach
    void setUp() {
        validator = new AiGatewayResponseValidator();

        AiRequestedPeriod period = new AiRequestedPeriod(OffsetDateTime.now().minusDays(7), OffsetDateTime.now());

        AiGlucoseSummary glucoseSummary = new AiGlucoseSummary(
                "glucose-summary:selected-period",
                "mg/dL",
                10,
                110.0,
                75.0,
                160.0,
                2,
                1
        );

        AiAdherenceSummary adherenceSummary = new AiAdherenceSummary(
                "adherence-summary:selected-period",
                7,
                6,
                1,
                1
        );

        AiAlertContext alert = new AiAlertContext(
                "alert-event:ref-001",
                "TEMP_HIGH",
                "CRITICAL",
                "OPEN",
                OffsetDateTime.now()
        );

        AiSelectedEvent event = new AiSelectedEvent(
                "glucose-event:ref-002",
                "GLUCOSE_HIGH",
                OffsetDateTime.now(),
                185.0,
                "mg/dL",
                "HIGH",
                "Glucose reading: 185.0 mg/dL"
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
                "The patient had an elevated glucose reading during the monitored period.",
                List.of("glucose-event:ref-002")
        );

        AiCorrelation corr = new AiCorrelation(
                "Storage temperature excursions correspond with lower recorded adherence.",
                "MEDIUM",
                List.of("adherence-summary:selected-period", "alert-event:ref-001")
        );

        AiProviderMetadata metadata = new AiProviderMetadata(
                "mock",
                "mock-model",
                promptVersion
        );

        validResponse = new AiClinicalSummaryGatewayResponse(
                requestId,
                "Clinical summary overview: patient demonstrates steady metrics.",
                List.of(obs),
                List.of(corr),
                List.of("Telemetry gaps may affect aggregate accuracy."),
                List.of("Discussion point on storage monitoring."),
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
    void shouldThrowWhenProviderIsNotMock() {
        AiProviderMetadata badMeta = new AiProviderMetadata("openai", "gpt-4", promptVersion);
        AiClinicalSummaryGatewayResponse badResponse = new AiClinicalSummaryGatewayResponse(
                requestId,
                validResponse.summary(),
                validResponse.observations(),
                validResponse.correlations(),
                validResponse.uncertainties(),
                validResponse.discussionPoints(),
                validResponse.safetyNotice(),
                badMeta
        );
        assertThrows(AiInvalidResponseException.class, () -> validator.validateResponse(validRequest, badResponse));
    }

    @Test
    void shouldThrowWhenUncertaintiesIsEmpty() {
        AiClinicalSummaryGatewayResponse badResponse = new AiClinicalSummaryGatewayResponse(
                requestId,
                validResponse.summary(),
                validResponse.observations(),
                validResponse.correlations(),
                Collections.emptyList(),
                validResponse.discussionPoints(),
                validResponse.safetyNotice(),
                validResponse.providerMetadata()
        );
        assertThrows(AiInvalidResponseException.class, () -> validator.validateResponse(validRequest, badResponse));
    }

    @Test
    void shouldThrowWhenUncitedEvidenceReferenced() {
        AiObservation badObs = new AiObservation(
                "Statement with uncited evidence",
                List.of("glucose-event:ref-999") // 999 does not exist in request!
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
    void shouldThrowWhenInvalidCategoryEvidenceReferenced() {
        AiObservation badObs = new AiObservation(
                "Statement with invalid category",
                List.of("unsupported-category:ref-001")
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
    void shouldThrowWhenCorrelationCitesFewerThanTwoCitations() {
        AiCorrelation badCorr = new AiCorrelation(
                "Correlation with only one citation",
                "HIGH",
                List.of("adherence-summary:selected-period")
        );
        AiClinicalSummaryGatewayResponse badResponse = new AiClinicalSummaryGatewayResponse(
                requestId,
                validResponse.summary(),
                validResponse.observations(),
                List.of(badCorr),
                validResponse.uncertainties(),
                validResponse.discussionPoints(),
                validResponse.safetyNotice(),
                validResponse.providerMetadata()
        );
        assertThrows(AiInvalidResponseException.class, () -> validator.validateResponse(validRequest, badResponse));
    }

    @Test
    void shouldThrowWhenUnsupportedConfidenceLevelProvided() {
        AiCorrelation badCorr = new AiCorrelation(
                "Correlation with invalid confidence",
                "EXTREMELY_CONFIDENT",
                List.of("adherence-summary:selected-period", "alert-event:ref-001")
        );
        AiClinicalSummaryGatewayResponse badResponse = new AiClinicalSummaryGatewayResponse(
                requestId,
                validResponse.summary(),
                validResponse.observations(),
                List.of(badCorr),
                validResponse.uncertainties(),
                validResponse.discussionPoints(),
                validResponse.safetyNotice(),
                validResponse.providerMetadata()
        );
        assertThrows(AiInvalidResponseException.class, () -> validator.validateResponse(validRequest, badResponse));
    }

    @Test
    void shouldThrowWhenDosageAdjustmentInText() {
        AiClinicalSummaryGatewayResponse badResponse = new AiClinicalSummaryGatewayResponse(
                requestId,
                "Please increase your insulin dose by 2 units.",
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
    void shouldThrowWhenDiagnosisInText() {
        AiClinicalSummaryGatewayResponse badResponse = new AiClinicalSummaryGatewayResponse(
                requestId,
                "The patient is diagnosed with type 2 diabetes complications.",
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
    void shouldThrowWhenDefiniteCausationInText() {
        AiClinicalSummaryGatewayResponse badResponse = new AiClinicalSummaryGatewayResponse(
                requestId,
                "High temperature was definitely caused by door open status.",
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
