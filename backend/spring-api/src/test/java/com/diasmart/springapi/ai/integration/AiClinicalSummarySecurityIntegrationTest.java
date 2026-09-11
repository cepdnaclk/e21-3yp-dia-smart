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
import com.diasmart.springapi.relationships.repository.UserPatientAccessRepository;
import com.diasmart.springapi.relationships.service.PatientAccessService;
import com.diasmart.springapi.shared.enums.AccessStatus;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.shared.security.AuthorizationService;
import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.entity.AppUser;
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
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * End-to-end security integration test for the AI Clinical Summary endpoint.
 *
 * Verifies the authorization chain:
 * AiClinicalSummaryController -> AuthorizationService -> PatientAccessService -> UserPatientAccessRepository
 *
 * Guarantees that any unauthorized request is rejected immediately with HTTP 403,
 * and that the downstream AI Gateway / Patient Context service is NEVER invoked.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiClinicalSummarySecurityIntegrationTest {

    @Mock private UserPatientAccessRepository userPatientAccessRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private PatientAiContextService patientAiContextService;
    @Mock private AiGatewayClient aiGatewayClient;
    @Mock private AiGatewayResponseValidator aiGatewayResponseValidator;
    @Mock private AuditService auditService;
    @Mock private AiProperties aiProperties;

    private PatientAccessService patientAccessService;
    private AuthorizationService authorizationService;
    private AiClinicalSummaryController controller;

    private final Long patientId = 1L;
    private final String validFrom = "2026-07-01T00:00:00Z";
    private final String validTo = "2026-07-07T00:00:00Z";

    @BeforeEach
    void setUp() {
        // Wire real authorization service chain
        patientAccessService = new PatientAccessService(currentUserService, userPatientAccessRepository);
        authorizationService = new AuthorizationService(patientAccessService);

        // Configure AI properties
        when(aiProperties.isEnabled()).thenReturn(true);
        when(aiProperties.getGatewayUrl()).thenReturn("http://127.0.0.1:8000");
        when(aiProperties.getInternalServiceToken()).thenReturn("mock-service-token");
        when(aiProperties.getMaxDateRangeDays()).thenReturn(31);

        controller = new AiClinicalSummaryController(
                patientAiContextService,
                aiGatewayClient,
                aiGatewayResponseValidator,
                auditService,
                currentUserService,
                authorizationService,
                aiProperties
        );

        // Setup mock gateway request/response for authorized cases
        UUID requestId = UUID.randomUUID();
        AiClinicalSummaryGatewayRequest request = new AiClinicalSummaryGatewayRequest(
                requestId, "CLINICAL_SUMMARY", "clinical-summary-v1", "pseudo-ref-123",
                null, null, null, null, null, Collections.emptyList(), Collections.emptyList()
        );
        when(patientAiContextService.buildGatewayRequest(eq(patientId), any(), any())).thenReturn(request);

        AiProviderMetadata providerMetadata = new AiProviderMetadata("mock", "mock-model", "clinical-summary-v1");
        AiClinicalSummaryGatewayResponse gatewayResponse = new AiClinicalSummaryGatewayResponse(
                requestId,
                "Normal glycemic stability observed.",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of("Confidence bounded by 7-day data"),
                Collections.emptyList(),
                AiGatewayResponseValidator.APPROVED_SAFETY_NOTICE,
                providerMetadata
        );
        when(aiGatewayClient.requestClinicalSummary(request)).thenReturn(gatewayResponse);
    }

    private AppUser createUser(Long userId, String email, UserRole role) {
        AppUser user = new AppUser();
        user.setUserId(userId);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    @Test
    @DisplayName("Patient self-access: user_id=2, role=PATIENT, active access -> 200 OK, gateway called")
    void shouldAllowPatientSelfAccess() {
        AppUser patientUser = createUser(2L, "patient@diasmart.com", UserRole.PATIENT);
        when(currentUserService.getCurrentUser()).thenReturn(patientUser);
        when(userPatientAccessRepository.existsByUserIdAndPatientIdAndStatusAndCanViewTrue(2L, patientId, AccessStatus.ACTIVE))
                .thenReturn(true);

        ResponseEntity<AiClinicalSummaryApiResponse> response = controller.getPatientAiSummary(patientId, validFrom, validTo);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(aiGatewayClient, times(1)).requestClinicalSummary(any());
        verify(patientAiContextService, times(1)).buildGatewayRequest(eq(patientId), any(), any());
    }

    @Test
    @DisplayName("Authorized caregiver: user_id=3, role=CAREGIVER, active access -> 200 OK, gateway called")
    void shouldAllowAuthorizedCaregiverAccess() {
        AppUser caregiverUser = createUser(3L, "caregiver@diasmart.com", UserRole.CAREGIVER);
        when(currentUserService.getCurrentUser()).thenReturn(caregiverUser);
        when(userPatientAccessRepository.existsByUserIdAndPatientIdAndStatusAndCanViewTrue(3L, patientId, AccessStatus.ACTIVE))
                .thenReturn(true);

        ResponseEntity<AiClinicalSummaryApiResponse> response = controller.getPatientAiSummary(patientId, validFrom, validTo);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(aiGatewayClient, times(1)).requestClinicalSummary(any());
    }

    @Test
    @DisplayName("Authorized doctor: user_id=4, role=DOCTOR, active access -> 200 OK, gateway called")
    void shouldAllowAuthorizedDoctorAccess() {
        AppUser doctorUser = createUser(4L, "doctor@diasmart.com", UserRole.DOCTOR);
        when(currentUserService.getCurrentUser()).thenReturn(doctorUser);
        when(userPatientAccessRepository.existsByUserIdAndPatientIdAndStatusAndCanViewTrue(4L, patientId, AccessStatus.ACTIVE))
                .thenReturn(true);

        ResponseEntity<AiClinicalSummaryApiResponse> response = controller.getPatientAiSummary(patientId, validFrom, validTo);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(aiGatewayClient, times(1)).requestClinicalSummary(any());
    }

    @Test
    @DisplayName("System admin: user_id=1, role=ADMIN -> 200 OK, gateway called without repository lookup")
    void shouldAllowAdminAccess() {
        AppUser adminUser = createUser(1L, "admin@diasmart.com", UserRole.ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);

        ResponseEntity<AiClinicalSummaryApiResponse> response = controller.getPatientAiSummary(patientId, validFrom, validTo);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(aiGatewayClient, times(1)).requestClinicalSummary(any());
        verifyNoInteractions(userPatientAccessRepository);
    }

    @Test
    @DisplayName("Unauthorized user: user_id=99, no relationship -> 403 AccessDeniedException, gateway NEVER called")
    void shouldRejectUnauthorizedUserAndNeverCallGateway() {
        AppUser stranger = createUser(99L, "stranger@diasmart.com", UserRole.PATIENT);
        when(currentUserService.getCurrentUser()).thenReturn(stranger);
        when(userPatientAccessRepository.existsByUserIdAndPatientIdAndStatusAndCanViewTrue(99L, patientId, AccessStatus.ACTIVE))
                .thenReturn(false);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                controller.getPatientAiSummary(patientId, validFrom, validTo)
        );
        assertEquals("You do not have access to this patient", ex.getMessage());

        verifyNoInteractions(aiGatewayClient);
        verifyNoInteractions(patientAiContextService);
    }

    @Test
    @DisplayName("Pending caregiver relationship: status=PENDING -> 403 AccessDeniedException, gateway NEVER called")
    void shouldRejectPendingCaregiverAndNeverCallGateway() {
        AppUser pendingCaregiver = createUser(5L, "pending@diasmart.com", UserRole.CAREGIVER);
        when(currentUserService.getCurrentUser()).thenReturn(pendingCaregiver);
        // Repository returns false because status is PENDING, not ACTIVE
        when(userPatientAccessRepository.existsByUserIdAndPatientIdAndStatusAndCanViewTrue(5L, patientId, AccessStatus.ACTIVE))
                .thenReturn(false);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                controller.getPatientAiSummary(patientId, validFrom, validTo)
        );
        assertEquals("You do not have access to this patient", ex.getMessage());

        verifyNoInteractions(aiGatewayClient);
        verifyNoInteractions(patientAiContextService);
    }

    @Test
    @DisplayName("Revoked relationship: status=REVOKED -> 403 AccessDeniedException, gateway NEVER called")
    void shouldRejectRevokedRelationshipAndNeverCallGateway() {
        AppUser revokedDoctor = createUser(6L, "revoked@diasmart.com", UserRole.DOCTOR);
        when(currentUserService.getCurrentUser()).thenReturn(revokedDoctor);
        when(userPatientAccessRepository.existsByUserIdAndPatientIdAndStatusAndCanViewTrue(6L, patientId, AccessStatus.ACTIVE))
                .thenReturn(false);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                controller.getPatientAiSummary(patientId, validFrom, validTo)
        );
        assertEquals("You do not have access to this patient", ex.getMessage());

        verifyNoInteractions(aiGatewayClient);
        verifyNoInteractions(patientAiContextService);
    }

    @Test
    @DisplayName("Caregiver with can_view=false -> 403 AccessDeniedException, gateway NEVER called")
    void shouldRejectCaregiverWithoutCanViewPermissionAndNeverCallGateway() {
        AppUser blindCaregiver = createUser(7L, "blind@diasmart.com", UserRole.CAREGIVER);
        when(currentUserService.getCurrentUser()).thenReturn(blindCaregiver);
        // Repository returns false because canView is false
        when(userPatientAccessRepository.existsByUserIdAndPatientIdAndStatusAndCanViewTrue(7L, patientId, AccessStatus.ACTIVE))
                .thenReturn(false);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                controller.getPatientAiSummary(patientId, validFrom, validTo)
        );
        assertEquals("You do not have access to this patient", ex.getMessage());

        verifyNoInteractions(aiGatewayClient);
        verifyNoInteractions(patientAiContextService);
    }
}
