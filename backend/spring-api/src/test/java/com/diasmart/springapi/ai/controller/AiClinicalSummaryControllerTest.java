package com.diasmart.springapi.ai.controller;

import com.diasmart.springapi.ai.client.AiGatewayClient;
import com.diasmart.springapi.ai.config.AiProperties;
import com.diasmart.springapi.ai.dto.api.AiClinicalSummaryApiResponse;
import com.diasmart.springapi.ai.dto.gateway.*;
import com.diasmart.springapi.ai.exception.AiConfigurationException;
import com.diasmart.springapi.ai.exception.AiDisabledException;
import com.diasmart.springapi.ai.exception.AiGatewayUnavailableException;
import com.diasmart.springapi.ai.service.PatientAiContextService;
import com.diasmart.springapi.ai.validation.AiGatewayResponseValidator;
import com.diasmart.springapi.audit.service.AuditService;
import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;
import com.diasmart.springapi.shared.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiClinicalSummaryControllerTest {

    @Mock private PatientAiContextService patientAiContextService;
    @Mock private AiGatewayClient aiGatewayClient;
    @Mock private AiGatewayResponseValidator aiGatewayResponseValidator;
    @Mock private AuditService auditService;
    @Mock private CurrentUserService currentUserService;
    @Mock private AuthorizationService authorizationService;
    @Mock private AiProperties aiProperties;

    @InjectMocks
    private AiClinicalSummaryController controller;

    private final Long patientId = 1L;
    private final String validFrom = "2026-07-01T00:00:00Z";
    private final String validTo = "2026-07-07T00:00:00Z";

    @BeforeEach
    void setUp() {
        when(aiProperties.isEnabled()).thenReturn(true);
        when(aiProperties.getGatewayUrl()).thenReturn("http://127.0.0.1:8000");
        when(aiProperties.getInternalServiceToken()).thenReturn("mock-service-token");
        when(aiProperties.getMaxDateRangeDays()).thenReturn(31);
        when(currentUserService.getCurrentUserId()).thenReturn(42L);
    }

    @Test
    void shouldThrowAccessDeniedWhenAuthorizationFailsAndNeverCallGateway() {
        doThrow(new AccessDeniedException("You do not have access to this patient"))
                .when(authorizationService).authorize(Permission.READ_PATIENT_READINGS, patientId);

        assertThrows(AccessDeniedException.class, () ->
                controller.getPatientAiSummary(patientId, validFrom, validTo)
        );

        verifyNoInteractions(aiGatewayClient);
        verifyNoInteractions(patientAiContextService);
    }

    @Test
    void shouldThrowAiDisabledExceptionWhenFeatureFlagIsFalseAndNeverCallGateway() {
        when(aiProperties.isEnabled()).thenReturn(false);

        AiDisabledException ex = assertThrows(AiDisabledException.class, () ->
                controller.getPatientAiSummary(patientId, validFrom, validTo)
        );
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        assertEquals("AI_DISABLED", ex.getErrorCode());

        verifyNoInteractions(aiGatewayClient);
        verifyNoInteractions(patientAiContextService);
    }

    @Test
    void shouldThrowAiConfigurationExceptionWhenConfigIsIncompleteAndNeverCallGateway() {
        when(aiProperties.getInternalServiceToken()).thenReturn("");

        AiConfigurationException ex = assertThrows(AiConfigurationException.class, () ->
                controller.getPatientAiSummary(patientId, validFrom, validTo)
        );
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        assertEquals("AI_CONFIGURATION_ERROR", ex.getErrorCode());

        verifyNoInteractions(aiGatewayClient);
        verifyNoInteractions(patientAiContextService);
    }

    @Test
    void shouldThrowBadRequestWhenTimezoneOffsetIsMissingAndNeverCallGateway() {
        String naiveFrom = "2026-07-01T00:00:00"; // Missing offset Z or +00:00

        ApiException ex = assertThrows(ApiException.class, () ->
                controller.getPatientAiSummary(patientId, naiveFrom, validTo)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_PERIOD", ex.getErrorCode());

        verifyNoInteractions(aiGatewayClient);
        verifyNoInteractions(patientAiContextService);
    }

    @Test
    void shouldThrowBadRequestWhenFromIsAfterToAndNeverCallGateway() {
        String badFrom = "2026-07-10T00:00:00Z";
        String badTo = "2026-07-01T00:00:00Z";

        ApiException ex = assertThrows(ApiException.class, () ->
                controller.getPatientAiSummary(patientId, badFrom, badTo)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_PERIOD", ex.getErrorCode());

        verifyNoInteractions(aiGatewayClient);
        verifyNoInteractions(patientAiContextService);
    }

    @Test
    void shouldThrowBadRequestWhenRangeExceedsLimitAndNeverCallGateway() {
        when(aiProperties.getMaxDateRangeDays()).thenReturn(5);

        ApiException ex = assertThrows(ApiException.class, () ->
                controller.getPatientAiSummary(patientId, validFrom, "2026-07-10T00:00:00Z")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_PERIOD", ex.getErrorCode());

        verifyNoInteractions(aiGatewayClient);
        verifyNoInteractions(patientAiContextService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnSummaryResponseOnSuccessfulRequestAndRecordSafeAudit() {
        UUID requestId = UUID.randomUUID();
        String pseudonymousRef = "patient-ref-synthetic-123";
        AiClinicalSummaryGatewayRequest request = new AiClinicalSummaryGatewayRequest(
                requestId, "CLINICAL_SUMMARY", "clinical-summary-v1", pseudonymousRef,
                null, null, null, null, null, Collections.emptyList(), Collections.emptyList()
        );
        when(patientAiContextService.buildGatewayRequest(eq(patientId), any(), any())).thenReturn(request);

        AiProviderMetadata providerMetadata = new AiProviderMetadata("mock", "mock-model", "clinical-summary-v1");
        AiClinicalSummaryGatewayResponse gatewayResponse = new AiClinicalSummaryGatewayResponse(
                requestId,
                "Clinical summary overview: patient shows good overall progress.",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of("Uncertainty details"),
                Collections.emptyList(),
                AiGatewayResponseValidator.APPROVED_SAFETY_NOTICE,
                providerMetadata
        );
        when(aiGatewayClient.requestClinicalSummary(request)).thenReturn(gatewayResponse);

        ResponseEntity<AiClinicalSummaryApiResponse> responseEntity = controller.getPatientAiSummary(patientId, validFrom, validTo);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(requestId, responseEntity.getBody().requestId());
        assertEquals("Clinical summary overview: patient shows good overall progress.", responseEntity.getBody().summary());

        verify(authorizationService).authorize(Permission.READ_PATIENT_READINGS, patientId);
        verify(aiGatewayResponseValidator).validateResponse(request, gatewayResponse);

        // Verify audit logging
        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService).record(eq(42L), eq(patientId), eq("AI_CLINICAL_SUMMARY_GENERATED"), eq("PATIENT"), eq(patientId), isNull(), detailsCaptor.capture());

        Map<String, Object> details = detailsCaptor.getValue();
        assertEquals(requestId, details.get("requestId"));
        assertEquals("SUCCESS", details.get("status"));
        assertEquals("mock", details.get("provider"));
        assertEquals("mock-model", details.get("model"));

        // Prove sensitive/pseudonymous references and clinical data are NOT recorded in audit details
        assertFalse(details.containsKey("pseudonymousRef"));
        assertFalse(details.containsKey("patientReference"));
        assertFalse(details.containsValue(pseudonymousRef));
        assertFalse(details.containsKey("glucoseValue"));
        assertFalse(details.containsKey("doseUnits"));
        assertFalse(details.containsKey("temperatureC"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRecordFailureAuditOnApiException() {
        when(patientAiContextService.buildGatewayRequest(eq(patientId), any(), any()))
                .thenThrow(new AiGatewayUnavailableException("AI Gateway is currently unavailable."));

        assertThrows(AiGatewayUnavailableException.class, () ->
                controller.getPatientAiSummary(patientId, validFrom, validTo)
        );

        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService).record(eq(42L), eq(patientId), eq("AI_CLINICAL_SUMMARY_FAILED"), eq("PATIENT"), eq(patientId), isNull(), detailsCaptor.capture());

        Map<String, Object> details = detailsCaptor.getValue();
        assertEquals("FAILURE", details.get("status"));
        assertEquals("AI_GATEWAY_UNAVAILABLE", details.get("errorCode"));
    }
}
