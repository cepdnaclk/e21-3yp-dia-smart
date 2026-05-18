package com.diasmart.springapi.admin.controller;

import com.diasmart.springapi.admin.dto.AdminCreateUserRequest;
import com.diasmart.springapi.admin.dto.AdminUpdateUserStatusRequest;
import com.diasmart.springapi.admin.service.AdminUserService;
import com.diasmart.springapi.shared.dto.ApiResponse;
import com.diasmart.springapi.users.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AdminUserController exposes admin-only user management APIs.
 *
 * Base path:
 * /api/v1/admin/users
 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * ADMIN creates a user account.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody AdminCreateUserRequest request) {
        UserResponse response = adminUserService.createUser(request);

        return ResponseEntity.ok(
                ApiResponse.success("User created successfully", response));
    }

    /**
     * ADMIN lists all users.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> response = adminUserService.getAllUsers();

        return ResponseEntity.ok(
                ApiResponse.success("Users retrieved successfully", response));
    }

    /**
     * ADMIN views one user.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable Long userId) {
        UserResponse response = adminUserService.getUserById(userId);

        return ResponseEntity.ok(
                ApiResponse.success("User retrieved successfully", response));
    }

    /**
     * ADMIN activates/deactivates one user.
     */
    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserStatusRequest request) {
        UserResponse response = adminUserService.updateUserStatus(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success("User status updated successfully", response));
    }
}