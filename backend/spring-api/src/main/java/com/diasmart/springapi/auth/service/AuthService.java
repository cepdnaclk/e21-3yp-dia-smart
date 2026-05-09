package com.diasmart.springapi.auth.service;

import com.diasmart.springapi.auth.dto.RegisterRequest;
import com.diasmart.springapi.shared.enums.AccountStatus;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.users.dto.UserResponse;
import com.diasmart.springapi.users.entity.AppUser;
import com.diasmart.springapi.users.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService contains authentication-related business logic.
 *
 * This step supports user registration.
 * Login and JWT generation will be added in the next step.
 */
@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new patient, caregiver, or doctor.
     *
     * Admin accounts are not allowed through public registration.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.getNormalizedEmail();

        if (request.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Admin accounts cannot be created through public registration");
        }

        if (appUserRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        AppUser user = new AppUser();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setPhoneNumber(request.getPhoneNumber());

        AppUser savedUser = appUserRepository.save(user);

        return UserResponse.fromEntity(savedUser);
    }
}