package com.diasmart.springapi.auth.security;

import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.users.entity.AppUser;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private AppUser user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(
                jwtService,
                "jwtSecret",
                "ThisIsAVeryLongSecretKeyForJwtTestingPurposes123456789"
        );

        ReflectionTestUtils.setField(
                jwtService,
                "jwtExpirationMs",
                3600000L
        );

        user = new AppUser();
        user.setUserId(1L);
        user.setUserUuid(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setRole(UserRole.PATIENT);
    }

    @Test
    void shouldGenerateToken() {
        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void shouldExtractUsername() {
        String token = jwtService.generateAccessToken(user);

        assertEquals(
                "test@example.com",
                jwtService.extractUsername(token)
        );
    }

    @Test
    void shouldValidateToken() {
        String token = jwtService.generateAccessToken(user);

        UserDetails details =
                User.withUsername("test@example.com")
                        .password("pw")
                        .authorities("ROLE_PATIENT")
                        .build();

        assertTrue(jwtService.isTokenValid(token, details));
    }

    @Test
    void shouldRejectTokenForDifferentUser() {
        String token = jwtService.generateAccessToken(user);

        UserDetails details =
                User.withUsername("other@example.com")
                        .password("pw")
                        .authorities("ROLE_PATIENT")
                        .build();

        assertFalse(jwtService.isTokenValid(token, details));
    }

    @Test
    void shouldFailWhenSecretMissing() {
        ReflectionTestUtils.setField(jwtService, "jwtSecret", null);

        assertThrows(
                IllegalStateException.class,
                () -> jwtService.generateAccessToken(user)
        );
    }

    @Test
    void shouldFailWhenSecretTooShort() {
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "short");

        assertThrows(
                IllegalStateException.class,
                () -> jwtService.generateAccessToken(user)
        );
    }

    @Test
    void shouldRejectInvalidToken() {
        assertThrows(
                JwtException.class,
                () -> jwtService.extractUsername("invalid.token")
        );
    }
}