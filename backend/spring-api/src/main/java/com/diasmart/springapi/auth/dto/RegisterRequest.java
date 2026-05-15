package com.diasmart.springapi.auth.dto;

import com.diasmart.springapi.shared.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * RegisterRequest represents public user registration.
 *
 * Schema-aligned fields:
 * - displayName maps to app_users.display_name
 * - contactNumber maps to app_users.contact_number
 *
 * JsonAlias keeps previous requests working temporarily:
 * - fullName -> displayName
 * - phoneNumber -> contactNumber
 */
public class RegisterRequest {

    @NotBlank(message = "Display name is required")
    @Size(max = 120, message = "Display name must not exceed 120 characters")
    @JsonAlias("fullName")
    private String displayName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @NotNull(message = "Role is required")
    private UserRole role;

    @Size(max = 30, message = "Contact number must not exceed 30 characters")
    @JsonAlias("phoneNumber")
    private String contactNumber;

    public RegisterRequest() {
    }

    public String getNormalizedEmail() {
        return email == null ? null : email.trim().toLowerCase();
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
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

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }
}