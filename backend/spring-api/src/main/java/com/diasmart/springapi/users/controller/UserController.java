package com.diasmart.springapi.users.controller;

import com.diasmart.springapi.shared.dto.ApiResponse;
import com.diasmart.springapi.users.dto.UserResponse;
import com.diasmart.springapi.users.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * UserController exposes user account APIs.
 *
 * Base path:
 * /api/v1/users
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns the currently logged-in user's profile.
     *
     * Endpoint:
     * GET /api/v1/users/me
     *
     * Requires:
     * Authorization: Bearer <JWT_TOKEN>
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile() {
        UserResponse currentUser = userService.getCurrentUserProfile();

        return ResponseEntity.ok(
                ApiResponse.success("Current user profile retrieved successfully", currentUser));
    }
}