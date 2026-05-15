package com.diasmart.springapi.users.dto;

import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.users.entity.AppUser;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Safe user response DTO.
 *
 * This DTO never exposes passwordHash.
 */
public class UserResponse {

    private Long userId;
    private UUID userUuid;
    private String email;
    private UserRole role;
    private String displayName;
    private String contactNumber;
    private boolean active;
    private OffsetDateTime lastLoginAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public UserResponse() {
    }

    public static UserResponse fromEntity(AppUser user) {
        UserResponse response = new UserResponse();

        response.setUserId(user.getUserId());
        response.setUserUuid(user.getUserUuid());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setDisplayName(user.getDisplayName());
        response.setContactNumber(user.getContactNumber());
        response.setActive(user.isActive());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        return response;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public UUID getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(UUID userUuid) {
        this.userUuid = userUuid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(OffsetDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}