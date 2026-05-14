package com.diasmart.springapi.users.dto;

import jakarta.validation.constraints.Size;

/**
 * Request body for updating the currently logged-in user's own profile.
 *
 * Endpoint:
 * PATCH /api/v1/users/me
 *
 * Email is not updated here because email is currently used as the JWT
 * identity.
 */
public class UpdateUserProfileRequest {

    @Size(max = 150, message = "Full name must not exceed 150 characters")
    private String fullName;

    @Size(max = 30, message = "Phone number must not exceed 30 characters")
    private String phoneNumber;

    public UpdateUserProfileRequest() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}