package com.diasmart.springapi.auth.service;

import com.diasmart.springapi.auth.dto.RegisterRequest;
import com.diasmart.springapi.shared.enums.AccountStatus;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.users.dto.UserResponse;
import com.diasmart.springapi.users.entity.AppUser;
import com.diasmart.springapi.users.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.diasmart.springapi.auth.dto.LoginRequest;
import com.diasmart.springapi.auth.dto.LoginResponse;
import com.diasmart.springapi.auth.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * AuthService contains authentication-related business logic.
 *
 * This step supports user registration.
 * Login and JWT generation will be added in the next step.
 */
@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new patient, caregiver, or doctor.
     *
     * Admin accounts are not allowed through public registration.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.getNormalizedEmail();

        if (request.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Admin accounts cannot be created through public registration");
        }

        if (appUserRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        AppUser user = new AppUser();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setPhoneNumber(request.getPhoneNumber());

        AppUser savedUser = appUserRepository.save(user);

        return UserResponse.fromEntity(savedUser);
    }

    /**
     * Authenticates a user and returns a JWT access token.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.getNormalizedEmail();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        String token = jwtService.generateAccessToken(user);

        return new LoginResponse(
                token,
                jwtService.getJwtExpirationMs(),
                UserResponse.fromEntity(user));
    }
}