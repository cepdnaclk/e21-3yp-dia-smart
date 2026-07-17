package com.diasmart.springapi.auth.service;

import com.diasmart.springapi.auth.dto.LoginRequest;
import com.diasmart.springapi.auth.dto.LoginResponse;
import com.diasmart.springapi.auth.dto.RegisterRequest;
import com.diasmart.springapi.auth.security.JwtService;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.users.dto.UserResponse;
import com.diasmart.springapi.users.entity.AppUser;
import com.diasmart.springapi.users.repository.AppUserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.diasmart.springapi.shared.exceptions.InvalidCredentialsException;
import org.springframework.security.core.AuthenticationException;

import java.time.OffsetDateTime;

import com.diasmart.springapi.patients.entity.Patient;
import com.diasmart.springapi.patients.repository.PatientRepository;
import com.diasmart.springapi.relationships.entity.UserPatientAccess;
import com.diasmart.springapi.relationships.repository.UserPatientAccessRepository;
import com.diasmart.springapi.shared.enums.AccessRole;
import com.diasmart.springapi.shared.enums.AccessStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    private final PatientRepository patientRepository;
    private final UserPatientAccessRepository userPatientAccessRepository;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            PatientRepository patientRepository,
            UserPatientAccessRepository userPatientAccessRepository) {

        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;

        this.patientRepository = patientRepository;
        this.userPatientAccessRepository = userPatientAccessRepository;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.getNormalizedEmail();

        if (request.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("ADMIN users cannot be registered publicly");
        }

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
        user.setActive(true);

        AppUser savedUser = appUserRepository.save(user);

        if (savedUser.getRole() == UserRole.PATIENT) {
            ensurePatientSelfAccess(savedUser);
        }

        return UserResponse.fromEntity(savedUser);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.getNormalizedEmail();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!user.isActive()) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        AppUser savedUser = appUserRepository.save(user);

        if (savedUser.getRole() == UserRole.PATIENT) {
            ensurePatientSelfAccess(savedUser);
        }

        String token = jwtService.generateAccessToken(savedUser);

        return new LoginResponse(
                token,
                jwtService.getJwtExpirationMs(),
                UserResponse.fromEntity(savedUser));
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