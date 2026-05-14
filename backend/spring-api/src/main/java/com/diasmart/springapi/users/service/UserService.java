package com.diasmart.springapi.users.service;

import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.dto.UpdateUserProfileRequest;
import com.diasmart.springapi.users.dto.UserResponse;
import com.diasmart.springapi.users.entity.AppUser;
import com.diasmart.springapi.users.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserService contains user-related business logic.
 *
 * Responsibilities:
 * - Retrieve the currently logged-in user's profile
 * - Update the currently logged-in user's own profile
 */
@Service
public class UserService {

    private final CurrentUserService currentUserService;
    private final AppUserRepository appUserRepository;

    public UserService(
            CurrentUserService currentUserService,
            AppUserRepository appUserRepository) {
        this.currentUserService = currentUserService;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        AppUser user = currentUserService.getCurrentUser();

        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse updateCurrentUserProfile(UpdateUserProfileRequest request) {
        AppUser user = currentUserService.getCurrentUser();

        if (request.getFullName() != null) {
            String fullName = request.getFullName().trim();

            if (fullName.isBlank()) {
                throw new IllegalArgumentException("Full name cannot be blank");
            }

            user.setFullName(fullName);
        }

        if (request.getPhoneNumber() != null) {
            String phoneNumber = request.getPhoneNumber().trim();

            if (phoneNumber.isBlank()) {
                user.setPhoneNumber(null);
            } else {
                user.setPhoneNumber(phoneNumber);
            }
        }

        AppUser savedUser = appUserRepository.save(user);

        return UserResponse.fromEntity(savedUser);
    }
}