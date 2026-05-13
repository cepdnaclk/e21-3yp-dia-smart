package com.diasmart.springapi.users.dto;

import com.diasmart.springapi.shared.enums.AccountStatus;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.users.entity.AppUser;

import java.time.Instant;

/**
 * UserResponse is the safe user data returned to the frontend.
 *
 * Important:
 * This DTO does NOT expose passwordHash.
 */
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private UserRole role;
    private AccountStatus accountStatus;
    private String phoneNumber;
    private Instant createdAt;
    private Instant updatedAt;

    public UserResponse() {
    }

    public UserResponse(
            Long id,
            String fullName,
            String email,
            UserRole role,
            AccountStatus accountStatus,
            String phoneNumber,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.accountStatus = accountStatus;
        this.phoneNumber = phoneNumber;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserResponse fromEntity(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getAccountStatus(),
                user.getPhoneNumber(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}