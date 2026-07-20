package com.diasmart.springapi.relationships.service;

import com.diasmart.springapi.relationships.dto.CreatePatientAccessRequest;
import com.diasmart.springapi.relationships.dto.PatientAccessResponse;
import com.diasmart.springapi.relationships.entity.UserPatientAccess;
import com.diasmart.springapi.relationships.repository.UserPatientAccessRepository;
import com.diasmart.springapi.relationships.entity.RelationshipRequest;
import com.diasmart.springapi.relationships.repository.RelationshipRequestRepository;
import com.diasmart.springapi.shared.enums.AccessStatus;
import com.diasmart.springapi.shared.enums.AccessRole;
import com.diasmart.springapi.shared.enums.RelationshipStatus;
import com.diasmart.springapi.shared.enums.RelationshipType;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.entity.AppUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * PatientAccessManagementService manages user-patient access relationships.
 *
 * PatientAccessService checks permissions.
 * PatientAccessManagementService creates, lists, and revokes relationships.
 */
@Service
public class PatientAccessManagementService {

    private final UserPatientAccessRepository userPatientAccessRepository;
    private final RelationshipRequestRepository relationshipRequestRepository;
    private final CurrentUserService currentUserService;

    public PatientAccessManagementService(
            UserPatientAccessRepository userPatientAccessRepository,
            RelationshipRequestRepository relationshipRequestRepository,
            CurrentUserService currentUserService) {
        this.userPatientAccessRepository = userPatientAccessRepository;
        this.relationshipRequestRepository = relationshipRequestRepository;
        this.currentUserService = currentUserService;
    }

    /**
     * Admin creates a user-patient relationship.
     */
    @Transactional
    public PatientAccessResponse createAccess(CreatePatientAccessRequest request) {
        requireAdminOrSelfDoctor(request.getUserId());
        PatientAccessResponse response = createAccessInternal(request);
        createMatchingRelationshipRequest(request);
        return response;
    }

    /**
     * Internal method to create patient access, bypassing admin authorization check.
     */
    @Transactional
    public PatientAccessResponse createAccessInternal(CreatePatientAccessRequest request) {
        /*
         * Correct behavior:
         *
         * Case 1: No row exists for this user + patient
         * -> create a new access row.
         *
         * Case 2: Row exists and status is ACTIVE
         * -> reject because active access already exists.
         *
         * Case 3: Row exists and status is REVOKED
         * -> reactivate the same row instead of inserting duplicate row.
         */
        UserPatientAccess access = userPatientAccessRepository
                .findByUserIdAndPatientId(
                        request.getUserId(),
                        request.getPatientId())
                .orElseGet(UserPatientAccess::new);

        if (access.getAccessId() != null && access.getStatus() == AccessStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Active access already exists for this user and patient");
        }

        access.setUserId(request.getUserId());
        access.setPatientId(request.getPatientId());
        access.setAccessRole(request.getAccessRole());
        access.setRelationshipLabel(
                normalizeNullableText(request.getRelationshipLabel()));

        access.setCanView(
                request.getCanView() == null || request.getCanView());

        access.setCanAcknowledgeAlerts(
                request.getCanAcknowledgeAlerts() != null
                        && request.getCanAcknowledgeAlerts());

        access.setCanEditPrescriptions(
                request.getCanEditPrescriptions() != null
                        && request.getCanEditPrescriptions());

        /*
         * If this was a previously revoked row, these lines reactivate it.
         */
        access.setStatus(AccessStatus.ACTIVE);
        access.setRevokedAt(null);
        access.setRevokedBy(null);

        UserPatientAccess savedAccess = userPatientAccessRepository.save(access);

        return PatientAccessResponse.fromEntity(savedAccess);
    }

    /**
     * Current user lists their own active access relationships.
     */
    @Transactional(readOnly = true)
    public List<PatientAccessResponse> getMyAccess() {
        AppUser currentUser = currentUserService.getCurrentUser();

        return userPatientAccessRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(
                        currentUser.getUserId(),
                        AccessStatus.ACTIVE)
                .stream()
                .map(PatientAccessResponse::fromEntity)
                .toList();
    }

    /**
     * Admin lists all access records for a user.
     */
    @Transactional(readOnly = true)
    public List<PatientAccessResponse> getAccessForUser(Long userId) {
        requireAdmin();

        return userPatientAccessRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PatientAccessResponse::fromEntity)
                .toList();
    }

    /**
     * Admin revokes a user-patient relationship.
     */
    @Transactional
    public PatientAccessResponse revokeAccess(Long accessId) {
        AppUser currentUser = requireAdmin();
        return revokeAccessInternal(accessId, currentUser.getUserId());
    }

    /**
     * Internal method to revoke patient access, bypassing admin authorization check.
     */
    @Transactional
    public PatientAccessResponse revokeAccessInternal(Long accessId, Long revokedByUserId) {
        UserPatientAccess access = userPatientAccessRepository
                .findById(accessId)
                .orElseThrow(() -> new IllegalArgumentException("Patient access record not found"));

        if (access.getStatus() == AccessStatus.REVOKED) {
            throw new IllegalArgumentException("Patient access is already revoked");
        }

        access.setStatus(AccessStatus.REVOKED);
        access.setRevokedAt(OffsetDateTime.now());
        access.setRevokedBy(revokedByUserId);

        UserPatientAccess savedAccess = userPatientAccessRepository.save(access);

        return PatientAccessResponse.fromEntity(savedAccess);
    }

    private AppUser requireAdmin() {
        AppUser currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only admins can manage patient access relationships");
        }

        return currentUser;
    }

    private void requireAdminOrSelfDoctor(Long targetUserId) {
        AppUser currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == UserRole.ADMIN) {
            return;
        }

        if (currentUser.getRole() == UserRole.DOCTOR && currentUser.getUserId().equals(targetUserId)) {
            return;
        }

        throw new AccessDeniedException("Only admins or doctors assigning themselves can manage patient access relationships");
    }

    private void createMatchingRelationshipRequest(CreatePatientAccessRequest request) {
        if (request.getAccessRole() != AccessRole.CAREGIVER && request.getAccessRole() != AccessRole.DOCTOR) {
            return;
        }

        Long patientUserId = userPatientAccessRepository
                .findByPatientIdAndAccessRoleAndStatus(request.getPatientId(), AccessRole.SELF, AccessStatus.ACTIVE)
                .map(UserPatientAccess::getUserId)
                .orElse(null);

        if (patientUserId == null) {
            return;
        }

        RelationshipType relRole = (request.getAccessRole() == AccessRole.CAREGIVER)
                ? RelationshipType.CAREGIVER
                : RelationshipType.DOCTOR;

        boolean requestExists = relationshipRequestRepository
                .existsByRequesterUserIdAndTargetUserIdAndPatientIdAndRelationshipRoleAndStatus(
                        request.getUserId(),
                        patientUserId,
                        request.getPatientId(),
                        relRole,
                        RelationshipStatus.ACCEPTED
                );

        if (!requestExists) {
            RelationshipRequest relRequest = new RelationshipRequest();
            relRequest.setRequesterUserId(request.getUserId());
            relRequest.setTargetUserId(patientUserId);
            relRequest.setPatientId(request.getPatientId());
            relRequest.setRelationshipRole(relRole);
            relRequest.setStatus(RelationshipStatus.ACCEPTED);
            relRequest.setMessage("Self-assigned via direct access configuration");
            relRequest.setRespondedAt(OffsetDateTime.now());
            relationshipRequestRepository.save(relRequest);
        }
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isBlank() ? null : trimmed;
    }
}