package com.diasmart.springapi.auth.controller;

import com.diasmart.springapi.auth.dto.RegisterRequest;
import com.diasmart.springapi.auth.service.AuthService;
import com.diasmart.springapi.shared.dto.ApiResponse;
import com.diasmart.springapi.users.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.diasmart.springapi.auth.dto.LoginRequest;
import com.diasmart.springapi.auth.dto.LoginResponse;

/**
 * AuthController exposes authentication-related REST APIs.
 *
 * Base path:
 * /api/v1/auth
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Endpoint:
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        UserResponse registeredUser = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account registered successfully", registeredUser));
    }

    /**
     * Endpoint:
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.login(request);

        return ResponseEntity
                .ok(ApiResponse.success("Login successful", loginResponse));
    }
}