package com.diasmart.springapi.users.controller;

import com.diasmart.springapi.shared.dto.ApiResponse;
import com.diasmart.springapi.users.dto.ChangePasswordRequest;
import com.diasmart.springapi.users.dto.UpdateUserProfileRequest;
import com.diasmart.springapi.users.dto.UserResponse;
import com.diasmart.springapi.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * UserController exposes current-user account APIs.
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

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile() {
        UserResponse currentUser = userService.getCurrentUserProfile();

        return ResponseEntity.ok(
                ApiResponse.success("Current user profile retrieved successfully", currentUser));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUserProfile(
            @Valid @RequestBody UpdateUserProfileRequest request) {
        UserResponse updatedUser = userService.updateCurrentUserProfile(request);

        return ResponseEntity.ok(
                ApiResponse.success("Current user profile updated successfully", updatedUser));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<UserResponse>> changeCurrentUserPassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        UserResponse updatedUser = userService.changeCurrentUserPassword(request);

        return ResponseEntity.ok(
                ApiResponse.success("Password changed successfully", updatedUser));
    }
}