package com.diasmart.springapi.admin.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body used by ADMIN to activate/deactivate a user.
 */
public class AdminUpdateUserStatusRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;

    public AdminUpdateUserStatusRequest() {
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}