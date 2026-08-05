package com.diasmart.springapi.ai.controller;

import com.diasmart.springapi.ai.client.AiGatewayClient;
import com.diasmart.springapi.ai.config.AiProperties;
import com.diasmart.springapi.ai.dto.api.AiClinicalSummaryApiResponse;
import com.diasmart.springapi.ai.dto.gateway.AiClinicalSummaryGatewayRequest;
import com.diasmart.springapi.ai.dto.gateway.AiClinicalSummaryGatewayResponse;
import com.diasmart.springapi.ai.dto.gateway.AiProviderMetadata;
import com.diasmart.springapi.ai.exception.AiDisabledException;
import com.diasmart.springapi.ai.service.PatientAiContextService;
import com.diasmart.springapi.ai.validation.AiGatewayResponseValidator;
import com.diasmart.springapi.audit.service.AuditService;
import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.shared.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiClinicalSummaryControllerTest {

    @Mock private PatientAiContextService patientAiContextService;
    @Mock private AiGatewayClient aiGatewayClient;
    @Mock private AiGatewayResponseValidator aiGatewayResponseValidator;
    @Mock private AuditService auditService;
    @Mock private CurrentUserService currentUserService;
    @Mock private AiProperties aiProperties;

    @InjectMocks
    private AiClinicalSummaryController controller;

    private final Long patientId = 1L;
    private final String validFrom = "2026-07-01T00:00:00Z";
    private final String validTo = "2026-07-07T00:00:00Z";

    @BeforeEach
    void setUp() {
        // Stub default enabled = true
        lenient().when(aiProperties.isEnabled()).thenReturn(true);
        lenient().when(aiProperties.getMaxDateRangeDays()).thenReturn(31);
    }

    @Test
    void shouldThrowAiDisabledExceptionWhenFeatureFlagIsFalse() {
        when(aiProperties.isEnabled()).thenReturn(false);

        assertThrows(AiDisabledException.class, () ->
                controller.getPatientAiSummary(patientId, validFrom, validTo)
        );
    }

    @Test
    void shouldThrowBadRequestWhenTimezoneOffsetIsMissing() {
        String naiveFrom = "2026-07-01T00:00:00"; // Missing offset Z or +00:00

        ApiException ex = assertThrows(ApiException.class, () ->
                controller.getPatientAiSummary(patientId, naiveFrom, validTo)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_PERIOD", ex.getErrorCode());
    }

    @Test
    void shouldThrowBadRequestWhenFromIsAfterTo() {
        String badFrom = "2026-07-10T00:00:00Z";
        String badTo = "2026-07-01T00:00:00Z";

        ApiException ex = assertThrows(ApiException.class, () ->
                controller.getPatientAiSummary(patientId, badFrom, badTo)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_PERIOD", ex.getErrorCode());
    }

    @Test
    void shouldThrowBadRequestWhenRangeExceedsLimit() {
        when(aiProperties.getMaxDateRangeDays()).thenReturn(5);

        ApiException ex = assertThrows(ApiException.class, () ->
                controller.getPatientAiSummary(patientId, validFrom, "2026-07-10T00:00:00Z")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_PERIOD", ex.getErrorCode());
    }

    @Test
    void shouldReturnSummaryResponseOnSuccessfulRequest() {
        UUID requestId = UUID.randomUUID();
        AiClinicalSummaryGatewayRequest request = new AiClinicalSummaryGatewayRequest(
                requestId, "CLINICAL_SUMMARY", "clinical-summary-v1", "patient-ref-123",
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

        // Verify validator was executed
        verify(aiGatewayResponseValidator).validateResponse(request, gatewayResponse);

        // Verify audit service was invoked
        verify(auditService).record(any(), eq(patientId), eq("AI_CLINICAL_SUMMARY_GENERATED"), eq("PATIENT"), eq(patientId), any(), any());
    }
}
