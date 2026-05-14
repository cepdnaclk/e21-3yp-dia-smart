package com.diasmart.springapi.users.service;

import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.dto.UserResponse;
import com.diasmart.springapi.users.entity.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserService contains user-related business logic.
 *
 * Current responsibility:
 * - Retrieve the currently logged-in user's profile.
 *
 * The current user is identified from the JWT-authenticated SecurityContext
 * through CurrentUserService.
 */
@Service
public class UserService {

    private final CurrentUserService currentUserService;

    public UserService(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    /**
     * Returns the profile of the currently logged-in user.
     *
     * Flow:
     * 1. JwtAuthenticationFilter validates the JWT token.
     * 2. Spring Security stores the authenticated user in SecurityContext.
     * 3. CurrentUserService reads the current user from SecurityContext.
     * 4. UserService converts the AppUser entity into a safe UserResponse DTO.
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        AppUser user = currentUserService.getCurrentUser();

        return UserResponse.fromEntity(user);
    }
}