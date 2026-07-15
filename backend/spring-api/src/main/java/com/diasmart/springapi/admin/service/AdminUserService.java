package com.diasmart.springapi.admin.service;

import com.diasmart.springapi.admin.dto.AdminCreateUserRequest;
import com.diasmart.springapi.admin.dto.AdminUpdateUserStatusRequest;
import com.diasmart.springapi.patients.entity.Patient;
import com.diasmart.springapi.patients.repository.PatientRepository;
import com.diasmart.springapi.relationships.entity.UserPatientAccess;
import com.diasmart.springapi.relationships.repository.UserPatientAccessRepository;
import com.diasmart.springapi.shared.enums.AccessRole;
import com.diasmart.springapi.shared.enums.AccessStatus;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.dto.UserResponse;
import com.diasmart.springapi.users.entity.AppUser;
import com.diasmart.springapi.users.repository.AppUserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * AdminUserService contains admin-only user management logic.
 */
@Service
public class AdminUserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final PatientRepository patientRepository;
    private final UserPatientAccessRepository userPatientAccessRepository;

    public AdminUserService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            CurrentUserService currentUserService,
            PatientRepository patientRepository,
            UserPatientAccessRepository userPatientAccessRepository) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
        this.patientRepository = patientRepository;
        this.userPatientAccessRepository = userPatientAccessRepository;
    }

    /**
     * ADMIN creates a new user account.
     */
    @Transactional
    public UserResponse createUser(AdminCreateUserRequest request) {
        requireAdmin();

        String email = request.getNormalizedEmail();

        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        String displayName = request.getDisplayName() == null
                ? null
                : request.getDisplayName().trim();

        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Display name is required");
        }

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setDisplayName(displayName);
        user.setContactNumber(normalizeNullableText(request.getContactNumber()));
        user.setActive(request.getActive() == null || request.getActive());

        AppUser savedUser = appUserRepository.save(user);

        if (savedUser.getRole() == UserRole.PATIENT) {
            ensurePatientSelfAccess(savedUser);
        }

        return UserResponse.fromEntity(savedUser);
    }

    /**
     * ADMIN lists all users.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        requireAdmin();

        return appUserRepository
                .findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    /**
     * ADMIN views one user by userId.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        requireAdmin();

        AppUser user = appUserRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return UserResponse.fromEntity(user);
    }

    /**
     * ADMIN activates/deactivates a user.
     */
    @Transactional
    public UserResponse updateUserStatus(
            Long userId,
            AdminUpdateUserStatusRequest request) {
        AppUser currentAdmin = requireAdmin();

        AppUser user = appUserRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (currentAdmin.getUserId().equals(user.getUserId())
                && Boolean.FALSE.equals(request.getActive())) {
            throw new IllegalArgumentException("Admin cannot deactivate their own account");
        }

        user.setActive(request.getActive());

        AppUser savedUser = appUserRepository.save(user);

        return UserResponse.fromEntity(savedUser);
    }

    private AppUser requireAdmin() {
        AppUser currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only admins can manage users");
        }

        return currentUser;
    }

    private void ensurePatientSelfAccess(AppUser user) {
        java.util.Optional<UserPatientAccess> existingSelfAccess = userPatientAccessRepository
                .findByUserIdOrderByCreatedAtDesc(user.getUserId())
                .stream()
                .filter(access -> access.getAccessRole() == AccessRole.SELF)
                .findFirst();

        if (existingSelfAccess.isPresent()) {
            UserPatientAccess access = existingSelfAccess.get();
            if (access.getStatus() != AccessStatus.ACTIVE) {
                access.setStatus(AccessStatus.ACTIVE);
                access.setCanView(true);
                access.setCanAcknowledgeAlerts(true);
                userPatientAccessRepository.save(access);
            }
        } else {
            Patient patient = new Patient();

            patient.setPatientUuid(UUID.randomUUID());
            patient.setFullName(user.getDisplayName());
            patient.setGender("UNKNOWN");
            patient.setDiabetesType("UNKNOWN");
            patient.setTargetGlucoseMinMgDl(BigDecimal.valueOf(70));
            patient.setTargetGlucoseMaxMgDl(BigDecimal.valueOf(140));
            patient.setActive(true);
            patient.setCreatedAt(OffsetDateTime.now());
            patient.setUpdatedAt(OffsetDateTime.now());

            Patient savedPatient = patientRepository.save(patient);

            if (savedPatient == null || savedPatient.getPatientId() == null) {
                throw new IllegalStateException("Failed to create a patient profile for the current user");
            }

            UserPatientAccess access = new UserPatientAccess();

            access.setUserId(user.getUserId());
            access.setPatientId(savedPatient.getPatientId());
            access.setAccessRole(AccessRole.SELF);
            access.setCanView(true);
            access.setCanAcknowledgeAlerts(true);
            access.setCanEditPrescriptions(false);
            access.setStatus(AccessStatus.ACTIVE);

            userPatientAccessRepository.save(access);
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