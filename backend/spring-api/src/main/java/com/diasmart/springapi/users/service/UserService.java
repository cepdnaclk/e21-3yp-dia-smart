package com.diasmart.springapi.users.service;

import com.diasmart.springapi.users.dto.UserResponse;
import com.diasmart.springapi.users.entity.AppUser;
import com.diasmart.springapi.users.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserService contains user-related business logic.
 *
 * In this step, it supports retrieving the currently authenticated user.
 */
@Service
public class UserService {

    private final AppUserRepository appUserRepository;

    public UserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /**
     * Returns the profile of the currently logged-in user.
     *
     * The user's email is extracted from Spring SecurityContext.
     * The SecurityContext is filled by JwtAuthenticationFilter
     * after validating the JWT token.
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("No authenticated user found");
        }

        String email = authentication.getName().trim().toLowerCase();

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found"));

        return UserResponse.fromEntity(user);
    }
}