package com.diasmart.springapi.users.service;

import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.dto.UpdateUserProfileRequest;
import com.diasmart.springapi.users.dto.UserResponse;
import com.diasmart.springapi.users.entity.AppUser;
import com.diasmart.springapi.users.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}