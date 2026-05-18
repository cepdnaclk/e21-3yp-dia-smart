package com.diasmart.springapi.admin.service;

import com.diasmart.springapi.admin.dto.AdminCreateUserRequest;
import com.diasmart.springapi.admin.dto.AdminUpdateUserStatusRequest;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.dto.UserResponse;
import com.diasmart.springapi.users.entity.AppUser;
import com.diasmart.springapi.users.repository.AppUserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AdminUserService contains admin-only user management logic.
 */
@Service
public class AdminUserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    public AdminUserService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            CurrentUserService currentUserService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
    }

    /**
     * ADMIN creates a new user account.
     */
    @Transactional
    public UserResponse createUser(AdminCreateUserRequest request) {
        requireAdmin();

        String email = request.getNormalizedEmail();

        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        String displayName = request.getDisplayName() == null
                ? null
                : request.getDisplayName().trim();

        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Display name is required");
        }

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setDisplayName(displayName);
        user.setContactNumber(normalizeNullableText(request.getContactNumber()));
        user.setActive(request.getActive() == null || request.getActive());

        AppUser savedUser = appUserRepository.save(user);

        return UserResponse.fromEntity(savedUser);
    }

    /**
     * ADMIN lists all users.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        requireAdmin();

        return appUserRepository
                .findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    /**
     * ADMIN views one user by userId.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        requireAdmin();

        AppUser user = appUserRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return UserResponse.fromEntity(user);
    }

    /**
     * ADMIN activates/deactivates a user.
     */
    @Transactional
    public UserResponse updateUserStatus(
            Long userId,
            AdminUpdateUserStatusRequest request) {
        AppUser currentAdmin = requireAdmin();

        AppUser user = appUserRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (currentAdmin.getUserId().equals(user.getUserId())
                && Boolean.FALSE.equals(request.getActive())) {
            throw new IllegalArgumentException("Admin cannot deactivate their own account");
        }

        user.setActive(request.getActive());

        AppUser savedUser = appUserRepository.save(user);

        return UserResponse.fromEntity(savedUser);
    }

    private AppUser requireAdmin() {
        AppUser currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only admins can manage users");
        }

        return currentUser;
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isBlank() ? null : trimmed;
    }
}