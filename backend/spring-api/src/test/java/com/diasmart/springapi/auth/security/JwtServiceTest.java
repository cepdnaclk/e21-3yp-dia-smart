package com.diasmart.springapi.auth.security;

import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.users.entity.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Tests")
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private AppUser testUser;
    private String validSecret;
    private Long expirationMs;

    @BeforeEach
    void setUp() {
        // Set a valid 32+ character secret
        validSecret = "ThisIsAVeryLongSecretKeyForJwtTestingPurposesOnly123456789";
        expirationMs = 3600000L; // 1 hour

        ReflectionTestUtils.setField(jwtService, "jwtSecret", validSecret);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", expirationMs);

        testUser = new AppUser();
        testUser.setUserId(1L);
        testUser.setUserUuid(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.PATIENT);
        testUser.setDisplayName("Test User");
        testUser.setActive(true);
        testUser.setCreatedAt(OffsetDateTime.now());
    }

    // =====================================================
    // TOKEN GENERATION TESTS
    // =====================================================

    @Test
    @DisplayName("Should generate valid access token")
    void testGenerateAccessTokenSuccess() {
        // Arrange & Act
        String token = jwtService.generateAccessToken(testUser);

        // Assert
        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(token.contains("."));
    }

    @Test
    @DisplayName("Should include user claims in token")
    void testTokenContainsUserClaims() {
        // Arrange & Act
        String token = jwtService.generateAccessToken(testUser);

        // Assert
        String username = jwtService.extractUsername(token);
        assertEquals("test@example.com", username);
    }

    @Test
    @DisplayName("Should include userId in token claims")
    void testTokenContainsUserId() {
        // Arrange & Act
        String token = jwtService.generateAccessToken(testUser);
        UserDetails userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities("ROLE_PATIENT")
                .build();

        // Assert
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    @DisplayName("Should include role in token claims")
    void testTokenContainsRole() {
        // Arrange & Act
        testUser.setRole(UserRole.DOCTOR);
        String token = jwtService.generateAccessToken(testUser);

        // Assert
        assertNotNull(token);
        UserDetails userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities("ROLE_DOCTOR")
                .build();
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    @DisplayName("Should set correct expiration time")
    void testTokenExpirationTime() {
        // Arrange
        Long customExpiration = 7200000L; // 2 hours
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", customExpiration);

        // Act
        String token = jwtService.generateAccessToken(testUser);

        // Assert
        UserDetails userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities("ROLE_PATIENT")
                .build();
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    // =====================================================
    // TOKEN VALIDATION TESTS
    // =====================================================

    @Test
    @DisplayName("Should validate correct token and user details")
    void testIsTokenValidSuccess() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);
        UserDetails userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities("ROLE_PATIENT")
                .build();

        // Act
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject token with mismatched username")
    void testIsTokenInvalidWithMismatchedUsername() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);
        UserDetails userDetails = User.builder()
                .username("different@example.com")
                .password("password")
                .authorities("ROLE_PATIENT")
                .build();

        // Act
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject expired token")
    void testIsTokenExpired() {
        // Arrange
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", -1000L); // Already expired
        String token = jwtService.generateAccessToken(testUser);
        
        UserDetails userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities("ROLE_PATIENT")
                .build();

        // Act & Assert
        assertThrows(ExpiredJwtException.class, () -> jwtService.isTokenValid(token, userDetails));
    }

    // =====================================================
    // USERNAME EXTRACTION TESTS
    // =====================================================

    @Test
    @DisplayName("Should extract username from token")
    void testExtractUsernameSuccess() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);

        // Act
        String username = jwtService.extractUsername(token);

        // Assert
        assertEquals("test@example.com", username);
    }

    @Test
    @DisplayName("Should extract correct email as username")
    void testExtractUsernameCorrectEmail() {
        // Arrange
        testUser.setEmail("patient@diasmart.com");
        String token = jwtService.generateAccessToken(testUser);

        // Act
        String username = jwtService.extractUsername(token);

        // Assert
        assertEquals("patient@diasmart.com", username);
    }

    // =====================================================
    // EXPIRATION TIME GETTER TESTS
    // =====================================================

    @Test
    @DisplayName("Should return correct JWT expiration milliseconds")
    void testGetJwtExpirationMs() {
        // Arrange & Act
        Long expiration = jwtService.getJwtExpirationMs();

        // Assert
        assertEquals(3600000L, expiration);
    }

    @Test
    @DisplayName("Should return expiration time with different configuration")
    void testGetJwtExpirationMsCustom() {
        // Arrange
        Long customExpiration = 7200000L;
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", customExpiration);

        // Act
        Long expiration = jwtService.getJwtExpirationMs();

        // Assert
        assertEquals(customExpiration, expiration);
    }

    // =====================================================
    // JWT SECRET VALIDATION TESTS
    // =====================================================

    @Test
    @DisplayName("Should throw exception when JWT secret is null")
    void testGenerateTokenNullSecret() {
        // Arrange
        ReflectionTestUtils.setField(jwtService, "jwtSecret", null);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> jwtService.generateAccessToken(testUser),
                "JWT secret is not configured");
    }

    @Test
    @DisplayName("Should throw exception when JWT secret is blank")
    void testGenerateTokenBlankSecret() {
        // Arrange
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "   ");

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> jwtService.generateAccessToken(testUser),
                "JWT secret is not configured");
    }

    @Test
    @DisplayName("Should throw exception when JWT secret is too short")
    void testGenerateTokenShortSecret() {
        // Arrange
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "tooshort");

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> jwtService.generateAccessToken(testUser),
                "JWT secret must be at least 32 characters long");
    }

    // =====================================================
    // EDGE CASES AND NULL INPUTS
    // =====================================================

    @Test
    @DisplayName("Should handle different user roles")
    void testTokenWithDifferentRoles() {
        // Arrange
        for (UserRole role : UserRole.values()) {
            testUser.setRole(role);

            // Act
            String token = jwtService.generateAccessToken(testUser);

            // Assert
            assertNotNull(token);
            UserDetails userDetails = User.builder()
                    .username("test@example.com")
                    .password("password")
                    .authorities("ROLE_" + role.name())
                    .build();
            assertTrue(jwtService.isTokenValid(token, userDetails));
        }
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void testDifferentTokensForDifferentUsers() {
        // Arrange
        AppUser user1 = new AppUser();
        user1.setUserId(1L);
        user1.setEmail("user1@example.com");
        user1.setRole(UserRole.PATIENT);

        AppUser user2 = new AppUser();
        user2.setUserId(2L);
        user2.setEmail("user2@example.com");
        user2.setRole(UserRole.PATIENT);

        // Act
        String token1 = jwtService.generateAccessToken(user1);
        String token2 = jwtService.generateAccessToken(user2);

        // Assert
        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("Should handle special characters in email")
    void testTokenWithSpecialCharactersEmail() {
        // Arrange
        testUser.setEmail("test+special@example.com");

        // Act
        String token = jwtService.generateAccessToken(testUser);

        // Assert
        String username = jwtService.extractUsername(token);
        assertEquals("test+special@example.com", username);
    }

    @Test
    @DisplayName("Should validate token with minimum secret length")
    void testTokenWithMinimumSecretLength() {
        // Arrange
        String minValidSecret = "a".repeat(32); // Exactly 32 characters
        ReflectionTestUtils.setField(jwtService, "jwtSecret", minValidSecret);

        // Act
        String token = jwtService.generateAccessToken(testUser);

        // Assert
        assertNotNull(token);
        UserDetails userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities("ROLE_PATIENT")
                .build();
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    @DisplayName("Should reject invalid token format")
    void testExtractUsernameInvalidFormat() {
        // Arrange
        String invalidToken = "not.a.valid.jwt.format";

        // Act & Assert
        assertThrows(JwtException.class, () -> jwtService.extractUsername(invalidToken));
    }

    @Test
    @DisplayName("Should handle token with ADMIN role")
    void testTokenWithAdminRole() {
        // Arrange
        testUser.setRole(UserRole.ADMIN);

        // Act
        String token = jwtService.generateAccessToken(testUser);

        // Assert
        assertNotNull(token);
        UserDetails userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities("ROLE_ADMIN")
                .build();
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    @DisplayName("Should handle token validation with case-sensitive username")
    void testTokenValidationCaseSensitive() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);
        UserDetails userDetails = User.builder()
                .username("TEST@EXAMPLE.COM") // Different case
                .password("password")
                .authorities("ROLE_PATIENT")
                .build();

        // Act
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should generate multiple valid tokens sequentially")
    void testGenerateMultipleTokens() {
        // Arrange & Act
        String token1 = jwtService.generateAccessToken(testUser);
        String token2 = jwtService.generateAccessToken(testUser);
        String token3 = jwtService.generateAccessToken(testUser);

        // Assert
        assertNotEquals(token1, token2);
        assertNotEquals(token2, token3);
        
        UserDetails userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities("ROLE_PATIENT")
                .build();
        
        assertTrue(jwtService.isTokenValid(token1, userDetails));
        assertTrue(jwtService.isTokenValid(token2, userDetails));
        assertTrue(jwtService.isTokenValid(token3, userDetails));
    }
}
