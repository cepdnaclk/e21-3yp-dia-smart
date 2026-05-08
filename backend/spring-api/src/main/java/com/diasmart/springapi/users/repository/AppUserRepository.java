package com.diasmart.springapi.users.repository;

import com.diasmart.springapi.users.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * AppUserRepository handles database operations for app_users.
 *
 * JpaRepository already gives us common methods like:
 * - save()
 * - findById()
 * - findAll()
 * - delete()
 *
 * We add custom methods needed for authentication.
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /**
     * Find a user by email.
     * This will be used during login.
     */
    Optional<AppUser> findByEmail(String email);

    /**
     * Check whether an email is already registered.
     * This will be used during registration.
     */
    boolean existsByEmail(String email);
}