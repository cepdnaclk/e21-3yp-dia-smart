package com.diasmart.springapi.relationships.service;

import com.diasmart.springapi.relationships.dto.CreateRelationshipRequestDto;
import com.diasmart.springapi.relationships.dto.RelationshipRequestDto;
import com.diasmart.springapi.relationships.dto.RelationshipSummaryDto;
import com.diasmart.springapi.relationships.entity.UserPatientAccess;
import com.diasmart.springapi.relationships.entity.RelationshipRequest;
import com.diasmart.springapi.relationships.repository.UserPatientAccessRepository;
import com.diasmart.springapi.relationships.repository.RelationshipRequestRepository;
import com.diasmart.springapi.relationships.dto.CreatePatientAccessRequest;
import com.diasmart.springapi.shared.enums.AccessRole;
import com.diasmart.springapi.shared.enums.AccessStatus;
import com.diasmart.springapi.shared.enums.RelationshipStatus;
import com.diasmart.springapi.shared.enums.RelationshipType;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.entity.AppUser;
import com.diasmart.springapi.users.repository.AppUserRepository;
import com.diasmart.springapi.patients.entity.Patient;
import com.diasmart.springapi.patients.repository.PatientRepository;
import com.diasmart.springapi.users.dto.UserResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RelationshipRequestService {

    private final RelationshipRequestRepository relationshipRequestRepository;
    private final UserPatientAccessRepository userPatientAccessRepository;
    private final AppUserRepository appUserRepository;
    private final PatientRepository patientRepository;
    private final CurrentUserService currentUserService;
    private final PatientAccessManagementService patientAccessManagementService;

    public RelationshipRequestService(
            RelationshipRequestRepository relationshipRequestRepository,
            UserPatientAccessRepository userPatientAccessRepository,
            AppUserRepository appUserRepository,
            PatientRepository patientRepository,
            CurrentUserService currentUserService,
            PatientAccessManagementService patientAccessManagementService) {
        this.relationshipRequestRepository = relationshipRequestRepository;
        this.userPatientAccessRepository = userPatientAccessRepository;
        this.appUserRepository = appUserRepository;
        this.patientRepository = patientRepository;
        this.currentUserService = currentUserService;
        this.patientAccessManagementService = patientAccessManagementService;
    }

    @Transactional
    public RelationshipRequestDto sendRequest(CreateRelationshipRequestDto dto) {
        AppUser currentUser = currentUserService.getCurrentUser();
        Long requesterUserId = currentUser.getUserId();

        Long targetUserId = dto.getTargetUserId();
        if (targetUserId == null && dto.getTargetEmail() != null) {
            AppUser targetUser = appUserRepository.findByEmailIgnoreCase(dto.getTargetEmail())
                    .orElseThrow(() -> new IllegalArgumentException("Target user with email " + dto.getTargetEmail() + " not found"));
            targetUserId = targetUser.getUserId();
        }

        Long patientId;
        if (currentUser.getRole() == UserRole.PATIENT) {
            List<UserPatientAccess> selfAccessList = userPatientAccessRepository.findByUserIdOrderByCreatedAtDesc(requesterUserId);
            patientId = selfAccessList.stream()
                    .filter(a -> a.getAccessRole() == AccessRole.SELF && a.getStatus() == AccessStatus.ACTIVE)
                    .map(UserPatientAccess::getPatientId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Patient profile not found for current user"));
        } else if (currentUser.getRole() == UserRole.CAREGIVER || currentUser.getRole() == UserRole.DOCTOR) {
            patientId = dto.getPatientId();
            if (patientId == null) {
                throw new IllegalArgumentException("Patient ID is required for caregiver or doctor requests");
            }
            if (!patientRepository.existsById(patientId)) {
                throw new IllegalArgumentException("Patient not found");
            }
            // Resolve targetUserId automatically from patientId if not provided
            if (targetUserId == null) {
                targetUserId = userPatientAccessRepository.findByPatientIdAndAccessRoleAndStatus(patientId, AccessRole.SELF, AccessStatus.ACTIVE)
                        .map(UserPatientAccess::getUserId)
                        .orElseThrow(() -> new IllegalArgumentException("Target user not found for the given patient profile"));
            }
        } else {
            throw new IllegalArgumentException("Admins cannot send relationship requests");
        }

        if (targetUserId == null) {
            throw new IllegalArgumentException("Target user ID or email is required");
        }

        if (requesterUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot send a relationship request to yourself");
        }

        // Prevent duplicate pending requests
        boolean pendingExists = relationshipRequestRepository.existsByRequesterUserIdAndTargetUserIdAndPatientIdAndRelationshipRoleAndStatus(
                requesterUserId, targetUserId, patientId, dto.getRelationshipRole(), RelationshipStatus.PENDING);
        if (pendingExists) {
            throw new IllegalArgumentException("A pending relationship request already exists");
        }

        // Prevent active relationship/access already existing for the same patient, target, and relationship role
        Long caregiverDoctorUserId = (currentUser.getRole() == UserRole.PATIENT) ? targetUserId : requesterUserId;
        AccessRole expectedAccessRole = (dto.getRelationshipRole() == RelationshipType.CAREGIVER) 
                ? AccessRole.CAREGIVER 
                : AccessRole.DOCTOR;

        boolean activeAccessExists = userPatientAccessRepository
                .findByUserIdAndPatientIdAndStatus(caregiverDoctorUserId, patientId, AccessStatus.ACTIVE)
                .map(access -> access.getAccessRole() == expectedAccessRole)
                .orElse(false);

        if (activeAccessExists) {
            throw new IllegalArgumentException("An active relationship or access already exists");
        }

        RelationshipRequest request = new RelationshipRequest();
        request.setRequesterUserId(requesterUserId);
        request.setTargetUserId(targetUserId);
        request.setPatientId(patientId);
        request.setRelationshipRole(dto.getRelationshipRole());
        request.setStatus(RelationshipStatus.PENDING);
        request.setMessage(dto.getMessage());

        RelationshipRequest savedRequest = relationshipRequestRepository.save(request);

        return RelationshipRequestDto.fromEntity(
                savedRequest,
                currentUser.getDisplayName(),
                getUserName(targetUserId),
                getPatientName(patientId)
        );
    }

    @Transactional
    public RelationshipRequestDto acceptRequest(Long requestId) {
        RelationshipRequest request = relationshipRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Relationship request not found"));

        if (request.getStatus() != RelationshipStatus.PENDING) {
            throw new IllegalArgumentException("Only pending requests can be accepted");
        }

        AppUser currentUser = currentUserService.getCurrentUser();
        if (!request.getTargetUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("Only the target user can accept this request");
        }

        request.setStatus(RelationshipStatus.ACCEPTED);
        request.setRespondedAt(OffsetDateTime.now());
        RelationshipRequest savedRequest = relationshipRequestRepository.save(request);

        AppUser requester = appUserRepository.findById(request.getRequesterUserId())
                .orElseThrow(() -> new IllegalArgumentException("Requester user not found"));
        AppUser target = appUserRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        Long userGettingAccessId;
        RelationshipType accessRoleType = request.getRelationshipRole();
        AccessRole mappedAccessRole = (accessRoleType == RelationshipType.CAREGIVER) ? AccessRole.CAREGIVER : AccessRole.DOCTOR;

        if (requester.getRole() == UserRole.CAREGIVER || requester.getRole() == UserRole.DOCTOR) {
            userGettingAccessId = request.getRequesterUserId();
        } else {
            userGettingAccessId = request.getTargetUserId();
        }

        CreatePatientAccessRequest accessRequest = new CreatePatientAccessRequest();
        accessRequest.setUserId(userGettingAccessId);
        accessRequest.setPatientId(request.getPatientId());
        accessRequest.setAccessRole(mappedAccessRole);
        accessRequest.setCanView(true);
        accessRequest.setCanAcknowledgeAlerts(true);
        accessRequest.setCanEditPrescriptions(mappedAccessRole == AccessRole.DOCTOR);

        patientAccessManagementService.createAccessInternal(accessRequest);

        return RelationshipRequestDto.fromEntity(
                savedRequest,
                requester.getDisplayName(),
                target.getDisplayName(),
                getPatientName(request.getPatientId())
        );
    }

    @Transactional
    public RelationshipRequestDto rejectRequest(Long requestId) {
        RelationshipRequest request = relationshipRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Relationship request not found"));

        if (request.getStatus() != RelationshipStatus.PENDING) {
            throw new IllegalArgumentException("Only pending requests can be rejected");
        }

        AppUser currentUser = currentUserService.getCurrentUser();
        if (!request.getTargetUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("Only the target user can reject this request");
        }

        request.setStatus(RelationshipStatus.REJECTED);
        request.setRespondedAt(OffsetDateTime.now());
        RelationshipRequest savedRequest = relationshipRequestRepository.save(request);

        return RelationshipRequestDto.fromEntity(
                savedRequest,
                getUserName(request.getRequesterUserId()),
                currentUser.getDisplayName(),
                getPatientName(request.getPatientId())
        );
    }

    @Transactional
    public RelationshipRequestDto revokeRelationship(Long requestId) {
        RelationshipRequest request = relationshipRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Relationship request not found"));

        if (request.getStatus() != RelationshipStatus.ACCEPTED) {
            throw new IllegalArgumentException("Only accepted relationships can be revoked");
        }

        AppUser currentUser = currentUserService.getCurrentUser();
        Long currentUserId = currentUser.getUserId();
        if (!request.getRequesterUserId().equals(currentUserId) && !request.getTargetUserId().equals(currentUserId)) {
            throw new AccessDeniedException("You are not authorized to revoke this relationship");
        }

        request.setStatus(RelationshipStatus.REVOKED);
        RelationshipRequest savedRequest = relationshipRequestRepository.save(request);

        Optional<UserPatientAccess> access1 = userPatientAccessRepository
                .findByUserIdAndPatientIdAndStatus(request.getRequesterUserId(), request.getPatientId(), AccessStatus.ACTIVE);
        Optional<UserPatientAccess> access2 = userPatientAccessRepository
                .findByUserIdAndPatientIdAndStatus(request.getTargetUserId(), request.getPatientId(), AccessStatus.ACTIVE);

        Long accessIdToRevoke = null;
        if (access1.isPresent() && access1.get().getAccessRole() != AccessRole.SELF) {
            accessIdToRevoke = access1.get().getAccessId();
        } else if (access2.isPresent() && access2.get().getAccessRole() != AccessRole.SELF) {
            accessIdToRevoke = access2.get().getAccessId();
        }

        if (accessIdToRevoke != null) {
            patientAccessManagementService.revokeAccessInternal(accessIdToRevoke, currentUserId);
        }

        return RelationshipRequestDto.fromEntity(
                savedRequest,
                getUserName(request.getRequesterUserId()),
                getUserName(request.getTargetUserId()),
                getPatientName(request.getPatientId())
        );
    }

    @Transactional(readOnly = true)
    public List<RelationshipRequestDto> getIncomingRequests() {
        Long currentUserId = currentUserService.getCurrentUserId();
        List<RelationshipRequest> requests = relationshipRequestRepository
                .findByTargetUserIdAndStatusOrderByCreatedAtDesc(currentUserId, RelationshipStatus.PENDING);

        return mapRequestsToDtos(requests);
    }

    @Transactional(readOnly = true)
    public List<RelationshipRequestDto> getSentRequests() {
        Long currentUserId = currentUserService.getCurrentUserId();
        List<RelationshipRequest> requests = relationshipRequestRepository
                .findByRequesterUserIdOrderByCreatedAtDesc(currentUserId);

        return mapRequestsToDtos(requests);
    }

    @Transactional(readOnly = true)
    public List<RelationshipSummaryDto> getMyRelationships() {
        Long currentUserId = currentUserService.getCurrentUserId();
        List<UserPatientAccess> selfAccessList = userPatientAccessRepository.findByUserIdOrderByCreatedAtDesc(currentUserId);
        Long patientId = selfAccessList.stream()
                .filter(a -> a.getAccessRole() == AccessRole.SELF && a.getStatus() == AccessStatus.ACTIVE)
                .map(UserPatientAccess::getPatientId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Patient profile not found for the current user"));

        List<RelationshipRequest> requests = relationshipRequestRepository
                .findByPatientIdAndStatusOrderByCreatedAtDesc(patientId, RelationshipStatus.ACCEPTED);

        return mapRequestsToSummaries(requests, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<RelationshipSummaryDto> getMyPatients() {
        Long currentUserId = currentUserService.getCurrentUserId();
        List<RelationshipRequest> requests = relationshipRequestRepository
                .findActiveRequestsForUser(currentUserId, RelationshipStatus.ACCEPTED);

        return mapRequestsToSummaries(requests, currentUserId);
    }

    private List<RelationshipRequestDto> mapRequestsToDtos(List<RelationshipRequest> requests) {
        if (requests.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        java.util.Set<Long> userIds = new java.util.HashSet<>();
        java.util.Set<Long> patientIds = new java.util.HashSet<>();
        for (RelationshipRequest r : requests) {
            if (r.getRequesterUserId() != null) userIds.add(r.getRequesterUserId());
            if (r.getTargetUserId() != null) userIds.add(r.getTargetUserId());
            if (r.getPatientId() != null) patientIds.add(r.getPatientId());
        }

        java.util.Map<Long, String> userNamesMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            appUserRepository.findAllById(userIds).forEach(user -> 
                userNamesMap.put(user.getUserId(), user.getDisplayName())
            );
        }

        java.util.Map<Long, String> patientNamesMap = new java.util.HashMap<>();
        if (!patientIds.isEmpty()) {
            patientRepository.findAllById(patientIds).forEach(patient -> 
                patientNamesMap.put(patient.getPatientId(), patient.getFullName())
            );
        }

        return requests.stream()
                .map(r -> RelationshipRequestDto.fromEntity(
                        r,
                        userNamesMap.getOrDefault(r.getRequesterUserId(), "Unknown User"),
                        userNamesMap.getOrDefault(r.getTargetUserId(), "Unknown User"),
                        patientNamesMap.getOrDefault(r.getPatientId(), "Unknown Patient")
                ))
                .toList();
    }

    private List<RelationshipSummaryDto> mapRequestsToSummaries(List<RelationshipRequest> requests, Long currentUserId) {
        if (requests.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        java.util.Set<Long> userIds = new java.util.HashSet<>();
        java.util.Set<Long> patientIds = new java.util.HashSet<>();
        for (RelationshipRequest r : requests) {
            Long otherUserId = r.getRequesterUserId().equals(currentUserId)
                    ? r.getTargetUserId()
                    : r.getRequesterUserId();
            if (otherUserId != null) userIds.add(otherUserId);
            if (r.getPatientId() != null) patientIds.add(r.getPatientId());
        }

        java.util.Map<Long, AppUser> usersMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            appUserRepository.findAllById(userIds).forEach(user -> 
                usersMap.put(user.getUserId(), user)
            );
        }

        java.util.Map<Long, String> patientNamesMap = new java.util.HashMap<>();
        if (!patientIds.isEmpty()) {
            patientRepository.findAllById(patientIds).forEach(patient -> 
                patientNamesMap.put(patient.getPatientId(), patient.getFullName())
            );
        }

        return requests.stream()
                .map(r -> {
                    RelationshipSummaryDto summary = new RelationshipSummaryDto();
                    summary.setRequestId(r.getRequestId());
                    summary.setPatientId(r.getPatientId());
                    summary.setPatientName(patientNamesMap.getOrDefault(r.getPatientId(), "Unknown Patient"));
                    summary.setRelationshipRole(r.getRelationshipRole());
                    summary.setCreatedAt(r.getCreatedAt());

                    Long otherUserId = r.getRequesterUserId().equals(currentUserId)
                            ? r.getTargetUserId()
                            : r.getRequesterUserId();

                    summary.setUserId(otherUserId);
                    AppUser otherUser = usersMap.get(otherUserId);
                    if (otherUser != null) {
                        summary.setDisplayName(otherUser.getDisplayName());
                        summary.setEmail(otherUser.getEmail());
                    } else {
                        summary.setDisplayName("Unknown User");
                    }
                    return summary;
                })
                .toList();
    }

    private String getUserName(Long userId) {
        if (userId == null) {
            return "N/A";
        }
        return appUserRepository.findById(userId)
                .map(AppUser::getDisplayName)
                .orElse("Unknown User");
    }

    private String getPatientName(Long patientId) {
        if (patientId == null) {
            return "N/A";
        }
        return patientRepository.findById(patientId)
                .map(Patient::getFullName)
                .orElse("Unknown Patient");
    }

    @Transactional(readOnly = true)
    public List<UserResponse> searchUsersByRole(UserRole role, String query) {
        List<AppUser> users;
        if (query == null || query.trim().isEmpty()) {
            users = appUserRepository.findByRoleAndActiveTrue(role);
        } else {
            users = appUserRepository.searchActiveByRoleAndQuery(role, query.trim());
        }
        return users.stream()
                .map(UserResponse::fromEntity)
                .toList();
    }
}
