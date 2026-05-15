package com.diasmart.springapi.users.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;

/**
 * Request body for updating the current user's profile.
 *
 * Schema-aligned:
 * - displayName maps to app_users.display_name
 * - contactNumber maps to app_users.contact_number
 */
public class UpdateUserProfileRequest {

    @Size(max = 120, message = "Display name must not exceed 120 characters")
    @JsonAlias("fullName")
    private String displayName;

    @Size(max = 30, message = "Contact number must not exceed 30 characters")
    @JsonAlias("phoneNumber")
    private String contactNumber;

    public UpdateUserProfileRequest() {
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }
}