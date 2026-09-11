package com.diasmart.springapi.ai.integration;

import com.diasmart.springapi.ai.client.AiGatewayClient;
import com.diasmart.springapi.ai.config.AiProperties;
import com.diasmart.springapi.ai.controller.AiClinicalSummaryController;
import com.diasmart.springapi.ai.dto.api.AiClinicalSummaryApiResponse;
import com.diasmart.springapi.ai.dto.gateway.AiClinicalSummaryGatewayRequest;
import com.diasmart.springapi.ai.dto.gateway.AiClinicalSummaryGatewayResponse;
import com.diasmart.springapi.ai.dto.gateway.AiProviderMetadata;
import com.diasmart.springapi.ai.service.PatientAiContextService;
import com.diasmart.springapi.ai.validation.AiGatewayResponseValidator;
import com.diasmart.springapi.audit.service.AuditService;
import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;
import com.diasmart.springapi.shared.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * End-to-end request validation integration test for the AI Clinical Summary endpoint.
 *
 * Verifies period parsing, chronological order, boundary validation (24h, 7d, 30d, 31d vs 32d),
 * and confirms that whenever validation fails with HTTP 400 BAD_REQUEST,
 * the downstream AI Gateway / Patient Context service is NEVER invoked.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiClinicalSummaryRequestValidationTest {

    @Mock private PatientAiContextService patientAiContextService;
    @Mock private AiGatewayClient aiGatewayClient;
    @Mock private AiGatewayResponseValidator aiGatewayResponseValidator;
    @Mock private AuditService auditService;
    @Mock private CurrentUserService currentUserService;
    @Mock private AuthorizationService authorizationService;
    @Mock private AiProperties aiProperties;

    private AiClinicalSummaryController controller;
    private final Long patientId = 1L;

    @BeforeEach
    void setUp() {
        when(aiProperties.isEnabled()).thenReturn(true);
        when(aiProperties.getGatewayUrl()).thenReturn("http://127.0.0.1:8000");
        when(aiProperties.getInternalServiceToken()).thenReturn("mock-service-token");
        when(aiProperties.getMaxDateRangeDays()).thenReturn(31);
        when(currentUserService.getCurrentUserId()).thenReturn(2L);

        controller = new AiClinicalSummaryController(
                patientAiContextService,
                aiGatewayClient,
                aiGatewayResponseValidator,
                auditService,
                currentUserService,
                authorizationService,
                aiProperties
        );

        UUID requestId = UUID.randomUUID();
        AiClinicalSummaryGatewayRequest request = new AiClinicalSummaryGatewayRequest(
                requestId, "CLINICAL_SUMMARY", "clinical-summary-v1", "pseudo-ref-123",
                null, null, null, null, null, Collections.emptyList(), Collections.emptyList()
        );
        when(patientAiContextService.buildGatewayRequest(eq(patientId), any(), any())).thenReturn(request);

        AiProviderMetadata providerMetadata = new AiProviderMetadata("mock", "mock-model", "clinical-summary-v1");
        AiClinicalSummaryGatewayResponse gatewayResponse = new AiClinicalSummaryGatewayResponse(
                requestId, "Valid summary", Collections.emptyList(), Collections.emptyList(),
                List.of("Uncertainty"), Collections.emptyList(),
                AiGatewayResponseValidator.APPROVED_SAFETY_NOTICE, providerMetadata
        );
        when(aiGatewayClient.requestClinicalSummary(any())).thenReturn(gatewayResponse);
    }

    @Test
    @DisplayName("Supported date range: 24 hours -> 200 OK")
    void shouldAccept24HourRange() {
        String from = "2026-07-06T00:00:00Z";
        String to = "2026-07-07T00:00:00Z";

        ResponseEntity<AiClinicalSummaryApiResponse> response = controller.getPatientAiSummary(patientId, from, to);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(aiGatewayClient, times(1)).requestClinicalSummary(any());
    }

    @Test
    @DisplayName("Supported date range: 7 days -> 200 OK")
    void shouldAccept7DayRange() {
        String from = "2026-07-01T00:00:00Z";
        String to = "2026-07-08T00:00:00Z";

        ResponseEntity<AiClinicalSummaryApiResponse> response = controller.getPatientAiSummary(patientId, from, to);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(aiGatewayClient, times(1)).requestClinicalSummary(any());
    }

    @Test
    @DisplayName("Supported date range: 30 days -> 200 OK")
    void shouldAccept30DayRange() {
        String from = "2026-06-01T00:00:00Z";
        String to = "2026-07-01T00:00:00Z";

        ResponseEntity<AiClinicalSummaryApiResponse> response = controller.getPatientAiSummary(patientId, from, to);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(aiGatewayClient, times(1)).requestClinicalSummary(any());
    }

    @Test
    @DisplayName("Supported maximum date range: exact 31 days boundary -> 200 OK")
    void shouldAcceptExact31DaysBoundary() {
        String from = "2026-07-01T00:00:00Z";
        String to = "2026-08-01T00:00:00Z"; // 31 days in July

        ResponseEntity<AiClinicalSummaryApiResponse> response = controller.getPatientAiSummary(patientId, from, to);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(aiGatewayClient, times(1)).requestClinicalSummary(any());
    }

    @Test
    @DisplayName("Exceeded range: 32 days (> 31 days max) -> 400 INVALID_PERIOD, gateway NEVER called")
    void shouldReject32DaysRangeAndNeverCallGateway() {
        String from = "2026-07-01T00:00:00Z";
        String to = "2026-08-02T00:00:00Z"; // 32 days

        ApiException ex = assertThrows(ApiException.class, () ->
                controller.getPatientAiSummary(patientId, from, to)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_PERIOD", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("exceeds the maximum limit of 31 days"));

        verifyNoInteractions(aiGatewayClient);
        verifyNoInteractions(patientAiContextService);
    }

    @Test
    @DisplayName("Inverted chronology: from is after to -> 400 INVALID_PERIOD, gateway NEVER called")
    void shouldRejectInvertedDatesAndNeverCallGateway() {
        String from = "2026-07-15T00:00:00Z";
        String to = "2026-07-01T00:00:00Z";

        ApiException ex = assertThrows(ApiException.class, () ->
                controller.getPatientAiSummary(patientId, from, to)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_PERIOD", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Start timestamp must be strictly before end timestamp"));

        verifyNoInteractions(aiGatewayClient);
        verifyNoInteractions(patientAiContextService);
    }

    @Test
    @DisplayName("Identical timestamps: from equals to -> 400 INVALID_PERIOD, gateway NEVER called")
    void shouldRejectEqualTimestampsAndNeverCallGateway() {
        String from = "2026-07-01T00:00:00Z";
        String to = "2026-07-01T00:00:00Z";

        ApiException ex = assertThrows(ApiException.class, () ->
                controller.getPatientAiSummary(patientId, from, to)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_PERIOD", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Start timestamp must be strictly before end timestamp"));

        verifyNoInteractions(aiGatewayClient);
        verifyNoInteractions(patientAiContextService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2026-07-01",                // Missing time and offset
            "2026-07-01T00:00:00",       // Missing timezone offset
            "not-a-timestamp",           // Non-numeric
            "07/01/2026 12:00:00",       // Non-ISO format
            ""                           // Empty string
    })
    @DisplayName("Malformed timestamp strings -> 400 INVALID_PERIOD, gateway NEVER called")
    void shouldRejectMalformedTimestampsAndNeverCallGateway(String malformedTimestamp) {
        ApiException ex = assertThrows(ApiException.class, () ->
                controller.getPatientAiSummary(patientId, malformedTimestamp, "2026-07-07T00:00:00Z")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_PERIOD", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("timezone-aware ISO-8601"));

        verifyNoInteractions(aiGatewayClient);
        verifyNoInteractions(patientAiContextService);
    }
}
