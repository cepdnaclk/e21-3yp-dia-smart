package com.diasmart.springapi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * LoginRequest represents the JSON body sent by the frontend
 * when a user logs in.
 *
 * Example:
 * {
 * "email": "john@example.com",
 * "password": "Password123"
 * }
 */
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    public LoginRequest() {
    }

    public String getEmail() {
        return email;
    }

    public String getNormalizedEmail() {
        return email == null ? null : email.trim().toLowerCase();
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}