package com.diasmart.springapi.relationships.service;

import com.diasmart.springapi.patients.entity.Patient;
import com.diasmart.springapi.patients.repository.PatientRepository;
import com.diasmart.springapi.relationships.dto.CreateRelationshipRequestDto;
import com.diasmart.springapi.relationships.dto.RelationshipRequestDto;
import com.diasmart.springapi.relationships.entity.RelationshipRequest;
import com.diasmart.springapi.relationships.entity.UserPatientAccess;
import com.diasmart.springapi.relationships.repository.RelationshipRequestRepository;
import com.diasmart.springapi.relationships.repository.UserPatientAccessRepository;
import com.diasmart.springapi.shared.enums.AccessRole;
import com.diasmart.springapi.shared.enums.AccessStatus;
import com.diasmart.springapi.shared.enums.RelationshipStatus;
import com.diasmart.springapi.shared.enums.RelationshipType;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.entity.AppUser;
import com.diasmart.springapi.users.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelationshipRequestServiceTest {

    @Mock
    private RelationshipRequestRepository relationshipRequestRepository;

    @Mock
    private UserPatientAccessRepository userPatientAccessRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private PatientAccessManagementService patientAccessManagementService;

    @InjectMocks
    private RelationshipRequestService relationshipRequestService;

    private AppUser patientUser;
    private AppUser caregiverUser;
    private AppUser doctorUser;
    private AppUser adminUser;
    private Patient patient;
    private UserPatientAccess selfAccess;
    private CreateRelationshipRequestDto requestDto;

    @BeforeEach
    void setUp() {
        patientUser = user(1L, "patient@example.com", UserRole.PATIENT, "Test Patient");
        caregiverUser = user(2L, "caregiver@example.com", UserRole.CAREGIVER, "Test Caregiver");
        doctorUser = user(3L, "doctor@example.com", UserRole.DOCTOR, "Test Doctor");
        adminUser = user(99L, "admin@example.com", UserRole.ADMIN, "Test Admin");

        patient = new Patient();
        patient.setPatientId(10L);
        patient.setFullName("Test Patient Profile");

        selfAccess = access(1L, 10L, AccessRole.SELF);

        requestDto = new CreateRelationshipRequestDto();
        requestDto.setTargetUserId(2L);
        requestDto.setRelationshipRole(RelationshipType.CAREGIVER);
        requestDto.setMessage("Please become my caregiver");
    }

    @Test
    void shouldSendRequestWhenPatientRequestIsValid() {
        when(currentUserService.getCurrentUser()).thenReturn(patientUser);
        when(userPatientAccessRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(selfAccess));
        mockNoPendingOrActiveRelationship(1L, 2L, 10L, RelationshipType.CAREGIVER, 2L);
        mockSaveWithId(100L);
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(caregiverUser));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));

        RelationshipRequestDto result = relationshipRequestService.sendRequest(requestDto);

        assertNotNull(result);
        assertEquals(1L, result.getRequesterUserId());
        assertEquals("Test Patient", result.getRequesterName());
        assertEquals(2L, result.getTargetUserId());
        assertEquals("Test Caregiver", result.getTargetName());
        assertEquals(10L, result.getPatientId());
        assertEquals("Test Patient Profile", result.getPatientName());
        assertEquals(RelationshipType.CAREGIVER, result.getRelationshipRole());
        assertEquals(RelationshipStatus.PENDING, result.getStatus());

        RelationshipRequest savedRequest = captureSavedRequest();
        assertEquals(1L, savedRequest.getRequesterUserId());
        assertEquals(2L, savedRequest.getTargetUserId());
        assertEquals(10L, savedRequest.getPatientId());
        assertEquals(RelationshipType.CAREGIVER, savedRequest.getRelationshipRole());
        assertEquals(RelationshipStatus.PENDING, savedRequest.getStatus());
        assertEquals("Please become my caregiver", savedRequest.getMessage());
    }

    @Test
    void shouldResolveTargetUsingEmail() {
        requestDto.setTargetUserId(null);
        requestDto.setTargetEmail("caregiver@example.com");

        when(currentUserService.getCurrentUser()).thenReturn(patientUser);
        when(appUserRepository.findByEmailIgnoreCase("caregiver@example.com"))
                .thenReturn(Optional.of(caregiverUser));
        when(userPatientAccessRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(selfAccess));
        mockNoPendingOrActiveRelationship(1L, 2L, 10L, RelationshipType.CAREGIVER, 2L);
        mockSaveWithId(101L);
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(caregiverUser));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));

        RelationshipRequestDto result = relationshipRequestService.sendRequest(requestDto);

        assertEquals(2L, result.getTargetUserId());
        assertEquals("Test Caregiver", result.getTargetName());
        assertEquals(2L, captureSavedRequest().getTargetUserId());
    }

    @Test
    void shouldSendRequestWhenCaregiverRequestIsValid() {
        requestDto.setTargetUserId(1L);
        requestDto.setPatientId(10L);

        when(currentUserService.getCurrentUser()).thenReturn(caregiverUser);
        when(patientRepository.existsById(10L)).thenReturn(true);
        mockNoPendingOrActiveRelationship(2L, 1L, 10L, RelationshipType.CAREGIVER, 2L);
        mockSaveWithId(102L);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(patientUser));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));

        RelationshipRequestDto result = relationshipRequestService.sendRequest(requestDto);

        assertEquals(2L, result.getRequesterUserId());
        assertEquals(1L, result.getTargetUserId());
        assertEquals("Test Caregiver", result.getRequesterName());
        assertEquals("Test Patient", result.getTargetName());
        assertEquals(RelationshipStatus.PENDING, result.getStatus());
    }

    @Test
    void shouldSendRequestWhenDoctorRequestIsValid() {
        requestDto.setTargetUserId(1L);
        requestDto.setPatientId(10L);
        requestDto.setRelationshipRole(RelationshipType.DOCTOR);
        requestDto.setMessage("Please connect me as your doctor");

        when(currentUserService.getCurrentUser()).thenReturn(doctorUser);
        when(patientRepository.existsById(10L)).thenReturn(true);
        mockNoPendingOrActiveRelationship(3L, 1L, 10L, RelationshipType.DOCTOR, 3L);
        mockSaveWithId(103L);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(patientUser));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));

        RelationshipRequestDto result = relationshipRequestService.sendRequest(requestDto);

        assertEquals(3L, result.getRequesterUserId());
        assertEquals(1L, result.getTargetUserId());
        assertEquals(RelationshipType.DOCTOR, result.getRelationshipRole());
        assertEquals("Please connect me as your doctor", result.getMessage());
    }

    @Test
    void shouldResolvePatientOwnerWhenTargetIsNotProvided() {
        requestDto.setTargetUserId(null);
        requestDto.setPatientId(10L);

        when(currentUserService.getCurrentUser()).thenReturn(caregiverUser);
        when(patientRepository.existsById(10L)).thenReturn(true);
        when(userPatientAccessRepository.findByPatientIdAndAccessRoleAndStatus(
                10L, AccessRole.SELF, AccessStatus.ACTIVE))
                .thenReturn(Optional.of(selfAccess));
        mockNoPendingOrActiveRelationship(2L, 1L, 10L, RelationshipType.CAREGIVER, 2L);
        mockSaveWithId(104L);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(patientUser));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));

        RelationshipRequestDto result = relationshipRequestService.sendRequest(requestDto);

        assertEquals(1L, result.getTargetUserId());
        assertEquals("Test Patient", result.getTargetName());
        assertEquals(1L, captureSavedRequest().getTargetUserId());
    }

    @Test
    void shouldRejectAdminRequest() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> relationshipRequestService.sendRequest(requestDto));

        assertEquals("Admins cannot send relationship requests", exception.getMessage());
        verify(relationshipRequestRepository, never()).save(any());
    }

    @Test
    void shouldRejectSelfRequest() {
        requestDto.setTargetUserId(1L);

        when(currentUserService.getCurrentUser()).thenReturn(patientUser);
        when(userPatientAccessRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(selfAccess));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> relationshipRequestService.sendRequest(requestDto));

        assertEquals("Cannot send a relationship request to yourself", exception.getMessage());
        verify(relationshipRequestRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicatePendingRequest() {
        when(currentUserService.getCurrentUser()).thenReturn(patientUser);
        when(userPatientAccessRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(selfAccess));
        when(relationshipRequestRepository.existsByRequesterUserIdAndTargetUserIdAndPatientIdAndRelationshipRoleAndStatus(
                1L, 2L, 10L, RelationshipType.CAREGIVER, RelationshipStatus.PENDING))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> relationshipRequestService.sendRequest(requestDto));

        assertEquals("A pending relationship request already exists", exception.getMessage());
        verify(relationshipRequestRepository, never()).save(any());
    }

    @Test
    void shouldRejectExistingActiveRelationship() {
        when(currentUserService.getCurrentUser()).thenReturn(patientUser);
        when(userPatientAccessRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(selfAccess));
        when(relationshipRequestRepository.existsByRequesterUserIdAndTargetUserIdAndPatientIdAndRelationshipRoleAndStatus(
                1L, 2L, 10L, RelationshipType.CAREGIVER, RelationshipStatus.PENDING))
                .thenReturn(false);
        when(userPatientAccessRepository.findByUserIdAndPatientIdAndStatus(2L, 10L, AccessStatus.ACTIVE))
                .thenReturn(Optional.of(access(2L, 10L, AccessRole.CAREGIVER)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> relationshipRequestService.sendRequest(requestDto));

        assertEquals("An active relationship or access already exists", exception.getMessage());
        verify(relationshipRequestRepository, never()).save(any());
    }

    @Test
    void shouldRejectCaregiverRequestWhenPatientIdIsMissing() {
        requestDto.setTargetUserId(1L);
        requestDto.setPatientId(null);

        when(currentUserService.getCurrentUser()).thenReturn(caregiverUser);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> relationshipRequestService.sendRequest(requestDto));

        assertEquals("Patient ID is required for caregiver or doctor requests", exception.getMessage());
        verify(relationshipRequestRepository, never()).save(any());
    }

    @Test
    void shouldRejectRequestWhenPatientDoesNotExist() {
        requestDto.setTargetUserId(1L);
        requestDto.setPatientId(10L);

        when(currentUserService.getCurrentUser()).thenReturn(caregiverUser);
        when(patientRepository.existsById(10L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> relationshipRequestService.sendRequest(requestDto));

        assertEquals("Patient not found", exception.getMessage());
        verify(relationshipRequestRepository, never()).save(any());
    }

    @Test
    void shouldRejectPatientRequestWhenSelfAccessDoesNotExist() {
        when(currentUserService.getCurrentUser()).thenReturn(patientUser);
        when(userPatientAccessRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(access(1L, 10L, AccessRole.CAREGIVER)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> relationshipRequestService.sendRequest(requestDto));

        assertEquals("Patient profile not found for current user", exception.getMessage());
        verify(relationshipRequestRepository, never()).save(any());
    }

    private AppUser user(Long userId, String email, UserRole role, String displayName) {
        AppUser user = new AppUser();
        user.setUserId(userId);
        user.setEmail(email);
        user.setRole(role);
        user.setDisplayName(displayName);
        user.setActive(true);
        return user;
    }

    private UserPatientAccess access(Long userId, Long patientId, AccessRole role) {
        UserPatientAccess access = new UserPatientAccess();
        access.setUserId(userId);
        access.setPatientId(patientId);
        access.setAccessRole(role);
        access.setStatus(AccessStatus.ACTIVE);
        return access;
    }

    private void mockNoPendingOrActiveRelationship(
            Long requesterUserId,
            Long targetUserId,
            Long patientId,
            RelationshipType relationshipType,
            Long caregiverDoctorUserId) {
        when(relationshipRequestRepository.existsByRequesterUserIdAndTargetUserIdAndPatientIdAndRelationshipRoleAndStatus(
                requesterUserId, targetUserId, patientId, relationshipType, RelationshipStatus.PENDING))
                .thenReturn(false);
        when(userPatientAccessRepository.findByUserIdAndPatientIdAndStatus(
                caregiverDoctorUserId, patientId, AccessStatus.ACTIVE))
                .thenReturn(Optional.empty());
    }

    private void mockSaveWithId(Long requestId) {
        when(relationshipRequestRepository.save(any(RelationshipRequest.class)))
                .thenAnswer(invocation -> {
                    RelationshipRequest request = invocation.getArgument(0);
                    request.setRequestId(requestId);
                    return request;
                });
    }

    private RelationshipRequest captureSavedRequest() {
        ArgumentCaptor<RelationshipRequest> captor = ArgumentCaptor.forClass(RelationshipRequest.class);
        verify(relationshipRequestRepository).save(captor.capture());
        return captor.getValue();
    }
}
