package com.diasmart.springapi.shared.security;

import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.users.entity.AppUser;
import com.diasmart.springapi.users.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    public CurrentUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public AppUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("No authenticated user found");
        }

        String email = authentication.getName().trim().toLowerCase();

        return appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found"));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public String getCurrentUserEmail() {
        return getCurrentUser().getEmail();
    }

    public UserRole getCurrentUserRole() {
        return getCurrentUser().getRole();
    }

    public boolean hasRole(UserRole role) {
        return getCurrentUser().getRole() == role;
    }
}