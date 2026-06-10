package com.diasmart.springapi.users.service;

import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.dto.ChangePasswordRequest;
import com.diasmart.springapi.users.dto.UpdateUserProfileRequest;
import com.diasmart.springapi.users.dto.UserResponse;
import com.diasmart.springapi.users.entity.AppUser;
import com.diasmart.springapi.users.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserService contains current-user account business logic.
 *
 * Responsibilities:
 * - Get current user profile
 * - Update current user profile
 * - Change current user password
 */
@Service
public class UserService {

    private final CurrentUserService currentUserService;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            CurrentUserService currentUserService,
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder) {
        this.currentUserService = currentUserService;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        AppUser user = currentUserService.getCurrentUser();

        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse updateCurrentUserProfile(UpdateUserProfileRequest request) {
        AppUser user = currentUserService.getCurrentUser();

        if (request.getDisplayName() != null) {
            String displayName = request.getDisplayName().trim();

            if (displayName.isBlank()) {
                throw new IllegalArgumentException("Display name cannot be blank");
            }

            user.setDisplayName(displayName);
        }

        if (request.getContactNumber() != null) {
            String contactNumber = request.getContactNumber().trim();

            if (contactNumber.isBlank()) {
                user.setContactNumber(null);
            } else {
                user.setContactNumber(contactNumber);
            }
        }

        AppUser savedUser = appUserRepository.save(user);

        return UserResponse.fromEntity(savedUser);
    }

    /**
     * Changes the currently logged-in user's password.
     *
     * Rules:
     * - User must provide the correct current password.
     * - New password must be different from current password.
     * - New password is stored as BCrypt hash, never plain text.
     */
    @Transactional
    public UserResponse changeCurrentUserPassword(ChangePasswordRequest request) {
        AppUser user = currentUserService.getCurrentUser();

        boolean currentPasswordMatches = passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPasswordHash());

        if (!currentPasswordMatches) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        boolean newPasswordSameAsCurrent = passwordEncoder.matches(
                request.getNewPassword(),
                user.getPasswordHash());

        if (newPasswordSameAsCurrent) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        AppUser savedUser = appUserRepository.save(user);

        return UserResponse.fromEntity(savedUser);
    }
}