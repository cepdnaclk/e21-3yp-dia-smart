package com.diasmart.springapi.auth.service;

import com.diasmart.springapi.auth.dto.LoginRequest;
import com.diasmart.springapi.auth.dto.LoginResponse;
import com.diasmart.springapi.auth.dto.RegisterRequest;
import com.diasmart.springapi.auth.security.JwtService;
import com.diasmart.springapi.patients.entity.Patient;
import com.diasmart.springapi.patients.repository.PatientRepository;
import com.diasmart.springapi.relationships.entity.UserPatientAccess;
import com.diasmart.springapi.relationships.repository.UserPatientAccessRepository;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.shared.exceptions.InvalidCredentialsException;
import com.diasmart.springapi.users.dto.UserResponse;
import com.diasmart.springapi.users.entity.AppUser;
import com.diasmart.springapi.users.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private UserPatientAccessRepository userPatientAccessRepository;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setEmail("test@example.com");
        validRegisterRequest.setPassword("SecurePassword123!");
        validRegisterRequest.setDisplayName("Test User");
        validRegisterRequest.setRole(UserRole.PATIENT);
        validRegisterRequest.setContactNumber("1234567890");

        validLoginRequest = new LoginRequest();
        validLoginRequest.setEmail("test@example.com");
        validLoginRequest.setPassword("SecurePassword123!");

        testUser = new AppUser();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedPassword");
        testUser.setRole(UserRole.PATIENT);
        testUser.setDisplayName("Test User");
        testUser.setActive(true);
        testUser.setUserUuid(UUID.randomUUID());
        testUser.setCreatedAt(OffsetDateTime.now());
    }

    // =====================================================
    // REGISTRATION TESTS
    // =====================================================

    @Test
    @DisplayName("Should successfully register new patient")
    void testRegisterPatientSuccess() {
        // Arrange
        when(appUserRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);
        
        Patient savedPatient = new Patient();
        savedPatient.setPatientId(1L);
        savedPatient.setPatientUuid(UUID.randomUUID());
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);
        when(userPatientAccessRepository.save(any(UserPatientAccess.class))).thenReturn(new UserPatientAccess());

        // Act
        UserResponse response = authService.register(validRegisterRequest);

        // Assert
        assertNotNull(response);
        verify(appUserRepository, times(1)).save(any(AppUser.class));
        verify(patientRepository, times(1)).save(any(Patient.class));
        verify(userPatientAccessRepository, times(1)).save(any(UserPatientAccess.class));
    }

    @Test
    @DisplayName("Should reject registration with duplicate email")
    void testRegisterDuplicateEmail() {
        // Arrange
        when(appUserRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.register(validRegisterRequest),
                "Email is already registered");
        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @Test
    @DisplayName("Should reject ADMIN role registration")
    void testRegisterAdminRoleRejected() {
        // Arrange
        validRegisterRequest.setRole(UserRole.ADMIN);
        when(appUserRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.register(validRegisterRequest),
                "ADMIN users cannot be registered publicly");
        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @Test
    @DisplayName("Should reject registration with blank display name")
    void testRegisterBlankDisplayName() {
        // Arrange
        validRegisterRequest.setDisplayName("   ");
        when(appUserRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.register(validRegisterRequest),
                "Display name is required");
    }

    @Test
    @DisplayName("Should reject registration with null display name")
    void testRegisterNullDisplayName() {
        // Arrange
        validRegisterRequest.setDisplayName(null);
        when(appUserRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.register(validRegisterRequest),
                "Display name is required");
    }

    @Test
    @DisplayName("Should register with null contact number")
    void testRegisterNullContactNumber() {
        // Arrange
        validRegisterRequest.setContactNumber(null);
        when(appUserRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);
        
        Patient savedPatient = new Patient();
        savedPatient.setPatientId(1L);
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);
        when(userPatientAccessRepository.save(any(UserPatientAccess.class))).thenReturn(new UserPatientAccess());

        // Act
        UserResponse response = authService.register(validRegisterRequest);

        // Assert
        assertNotNull(response);
        
        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        assertNull(userCaptor.getValue().getContactNumber());
    }

    @Test
    @DisplayName("Should normalize email to lowercase")
    void testRegisterEmailNormalization() {
        // Arrange
        validRegisterRequest.setEmail("Test@EXAMPLE.COM");
        when(appUserRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);
        
        Patient savedPatient = new Patient();
        savedPatient.setPatientId(1L);
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);
        when(userPatientAccessRepository.save(any(UserPatientAccess.class))).thenReturn(new UserPatientAccess());

        // Act
        authService.register(validRegisterRequest);

        // Assert
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(appUserRepository).existsByEmailIgnoreCase(emailCaptor.capture());
        assertEquals("test@example.com", emailCaptor.getValue());
    }

    // =====================================================
    // LOGIN TESTS
    // =====================================================

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testLoginSuccess() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(appUserRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(testUser));
        when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(any(AppUser.class))).thenReturn("jwt-token");
        when(jwtService.getJwtExpirationMs()).thenReturn(3600000L);

        // Act
        LoginResponse response = authService.login(validLoginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.getAccessToken());
        assertEquals(3600000L, response.getExpiresInMs());
        assertNotNull(response.getUser());
        verify(appUserRepository, times(1)).save(any(AppUser.class));
    }

    @Test
    @DisplayName("Should reject login with invalid credentials")
    void testLoginInvalidCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.core.AuthenticationException("Invalid credentials") {});

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> authService.login(validLoginRequest));
        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @Test
    @DisplayName("Should reject login for inactive user")
    void testLoginInactiveUser() {
        // Arrange
        AppUser inactiveUser = new AppUser();
        inactiveUser.setActive(false);
        inactiveUser.setEmail("inactive@example.com");
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(appUserRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(inactiveUser));

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> authService.login(validLoginRequest));
    }

    @Test
    @DisplayName("Should reject login when user not found after authentication")
    void testLoginUserNotFoundAfterAuth() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(appUserRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> authService.login(validLoginRequest));
    }

    @Test
    @DisplayName("Should update last login timestamp")
    void testLoginUpdatesLastLoginAt() {
        // Arrange
        OffsetDateTime beforeLogin = OffsetDateTime.now();
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(appUserRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(testUser));
        when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(any(AppUser.class))).thenReturn("jwt-token");
        when(jwtService.getJwtExpirationMs()).thenReturn(3600000L);

        // Act
        authService.login(validLoginRequest);

        // Assert
        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        assertNotNull(userCaptor.getValue().getLastLoginAt());
        assertTrue(userCaptor.getValue().getLastLoginAt().isAfter(beforeLogin));
    }

    @Test
    @DisplayName("Should normalize email on login")
    void testLoginEmailNormalization() {
        // Arrange
        validLoginRequest.setEmail("Test@EXAMPLE.COM");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(appUserRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(testUser));
        when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(any(AppUser.class))).thenReturn("jwt-token");
        when(jwtService.getJwtExpirationMs()).thenReturn(3600000L);

        // Act
        authService.login(validLoginRequest);

        // Assert
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(appUserRepository).findByEmailIgnoreCase(emailCaptor.capture());
        assertEquals("test@example.com", emailCaptor.getValue());
    }

    // =====================================================
    // EDGE CASES AND NULL INPUTS
    // =====================================================

    @Test
    @DisplayName("Should handle whitespace-only contact number")
    void testRegisterWhitespaceContactNumber() {
        // Arrange
        validRegisterRequest.setContactNumber("   ");
        when(appUserRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);
        
        Patient savedPatient = new Patient();
        savedPatient.setPatientId(1L);
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);
        when(userPatientAccessRepository.save(any(UserPatientAccess.class))).thenReturn(new UserPatientAccess());

        // Act
        UserResponse response = authService.register(validRegisterRequest);

        // Assert
        assertNotNull(response);
        
        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        assertNull(userCaptor.getValue().getContactNumber());
    }

    @Test
    @DisplayName("Should verify patient creation during registration")
    void testRegisterCreatesPatientRecord() {
        // Arrange
        when(appUserRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);
        
        Patient savedPatient = new Patient();
        savedPatient.setPatientId(1L);
        savedPatient.setPatientUuid(UUID.randomUUID());
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);
        when(userPatientAccessRepository.save(any(UserPatientAccess.class))).thenReturn(new UserPatientAccess());

        // Act
        authService.register(validRegisterRequest);

        // Assert
        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(patientCaptor.capture());
        
        Patient capturedPatient = patientCaptor.getValue();
        assertNotNull(capturedPatient.getPatientUuid());
        assertEquals(Boolean.TRUE, capturedPatient.getActive());
    }

    @Test
    @DisplayName("Should verify user-patient access creation during registration")
    void testRegisterCreatesAccessRecord() {
        // Arrange
        when(appUserRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);
        
        Patient savedPatient = new Patient();
        savedPatient.setPatientId(1L);
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);
        when(userPatientAccessRepository.save(any(UserPatientAccess.class))).thenReturn(new UserPatientAccess());

        // Act
        authService.register(validRegisterRequest);

        // Assert
        ArgumentCaptor<UserPatientAccess> accessCaptor = ArgumentCaptor.forClass(UserPatientAccess.class);
        verify(userPatientAccessRepository).save(accessCaptor.capture());
        
        UserPatientAccess capturedAccess = accessCaptor.getValue();
        assertEquals(testUser.getUserId(), capturedAccess.getUserId());
        assertEquals(1L, capturedAccess.getPatientId());
    }

    @Test
    @DisplayName("Should encode password during registration")
    void testRegisterEncodesPassword() {
        // Arrange
        when(appUserRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("encodedPassword");
        when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);
        
        Patient savedPatient = new Patient();
        savedPatient.setPatientId(1L);
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);
        when(userPatientAccessRepository.save(any(UserPatientAccess.class))).thenReturn(new UserPatientAccess());

        // Act
        authService.register(validRegisterRequest);

        // Assert
        verify(passwordEncoder).encode("SecurePassword123!");
    }

    @Test
    @DisplayName("Should not create patient for CAREGIVER role")
    void testRegisterNonPatientNoPatient() {
        // Arrange
        validRegisterRequest.setRole(UserRole.CAREGIVER);
        when(appUserRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);

        // Act
        UserResponse response = authService.register(validRegisterRequest);

        // Assert
        assertNotNull(response);
        verify(patientRepository, never()).save(any(Patient.class));
        verify(userPatientAccessRepository, never()).save(any(UserPatientAccess.class));
    }
}
