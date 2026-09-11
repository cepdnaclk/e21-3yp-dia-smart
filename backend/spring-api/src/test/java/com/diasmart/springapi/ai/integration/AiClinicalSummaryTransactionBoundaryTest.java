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
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;
import com.diasmart.springapi.shared.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Verifies that the AI Clinical Summary controller and HTTP gateway invocation
 * are strictly outside of any database transaction boundary.
 *
 * Outbound microservice HTTP requests must NEVER be executed within an open
 * database transaction to prevent connection pool exhaustion and thread starvation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiClinicalSummaryTransactionBoundaryTest {

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
    }

    @Test
    @DisplayName("Verify AiClinicalSummaryController class is NOT annotated with @Transactional")
    void verifyControllerClassIsNotTransactional() {
        assertNull(
                AiClinicalSummaryController.class.getAnnotation(Transactional.class),
                "AiClinicalSummaryController must NOT be annotated with @Transactional"
        );
    }

    @Test
    @DisplayName("Verify getPatientAiSummary endpoint method is NOT annotated with @Transactional")
    void verifyEndpointMethodIsNotTransactional() throws NoSuchMethodException {
        Method method = AiClinicalSummaryController.class.getMethod(
                "getPatientAiSummary",
                Long.class, String.class, String.class
        );
        assertNull(
                method.getAnnotation(Transactional.class),
                "getPatientAiSummary method must NOT be annotated with @Transactional"
        );
    }

    @Test
    @DisplayName("Verify no active transaction exists during aiGatewayClient invocation")
    void verifyNoActiveTransactionDuringGatewayCall() {
        UUID requestId = UUID.randomUUID();
        AiClinicalSummaryGatewayRequest request = new AiClinicalSummaryGatewayRequest(
                requestId, "CLINICAL_SUMMARY", "clinical-summary-v1", "pseudo-ref-123",
                null, null, null, null, null, Collections.emptyList(), Collections.emptyList()
        );
        when(patientAiContextService.buildGatewayRequest(eq(patientId), any(), any())).thenReturn(request);

        AtomicBoolean wasTransactionActiveDuringHttpCall = new AtomicBoolean(false);

        AiProviderMetadata providerMetadata = new AiProviderMetadata("mock", "mock-model", "clinical-summary-v1");
        AiClinicalSummaryGatewayResponse gatewayResponse = new AiClinicalSummaryGatewayResponse(
                requestId, "Valid summary", Collections.emptyList(), Collections.emptyList(),
                List.of("Uncertainty"), Collections.emptyList(),
                AiGatewayResponseValidator.APPROVED_SAFETY_NOTICE, providerMetadata
        );

        when(aiGatewayClient.requestClinicalSummary(any())).thenAnswer(invocation -> {
            // Check if Spring transaction manager reports an active transaction on the executing thread
            boolean active = TransactionSynchronizationManager.isActualTransactionActive();
            wasTransactionActiveDuringHttpCall.set(active);
            return gatewayResponse;
        });

        ResponseEntity<AiClinicalSummaryApiResponse> response = controller.getPatientAiSummary(
                patientId, "2026-07-01T00:00:00Z", "2026-07-07T00:00:00Z"
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(
                wasTransactionActiveDuringHttpCall.get(),
                "aiGatewayClient.requestClinicalSummary must be executed outside any active database transaction"
        );
    }
}
