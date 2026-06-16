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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private AppUser user;

    @BeforeEach
    void setUp() {

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setDisplayName("Test User");
        registerRequest.setRole(UserRole.PATIENT);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        user = new AppUser();
        user.setUserId(1L);
        user.setEmail("test@example.com");
        user.setRole(UserRole.PATIENT);
        user.setDisplayName("Test User");
        user.setActive(true);
    }

    @Test
    void shouldRegisterPatient() {

        when(appUserRepository.existsByEmailIgnoreCase(anyString()))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded");

        when(appUserRepository.save(any(AppUser.class)))
                .thenReturn(user);

        Patient patient = new Patient();
        patient.setPatientId(1L);

        when(patientRepository.save(any(Patient.class)))
                .thenReturn(patient);

        UserResponse response =
                authService.register(registerRequest);

        assertNotNull(response);

        verify(patientRepository).save(any(Patient.class));
        verify(userPatientAccessRepository)
                .save(any(UserPatientAccess.class));
    }

    @Test
    void shouldRejectDuplicateEmail() {

        when(appUserRepository.existsByEmailIgnoreCase(anyString()))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest)
        );
    }

    @Test
    void shouldRejectAdminRegistration() {

        registerRequest.setRole(UserRole.ADMIN);

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest)
        );
    }

    @Test
    void shouldRejectBlankDisplayName() {

        registerRequest.setDisplayName(" ");

        when(appUserRepository.existsByEmailIgnoreCase(anyString()))
                .thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest)
        );
    }

    @Test
    void shouldEncodePassword() {

        when(appUserRepository.existsByEmailIgnoreCase(anyString()))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded");

        when(appUserRepository.save(any(AppUser.class)))
                .thenReturn(user);

        Patient patient = new Patient();
        patient.setPatientId(1L);

        when(patientRepository.save(any(Patient.class)))
                .thenReturn(patient);

        authService.register(registerRequest);

        verify(passwordEncoder)
                .encode("password123");
    }

    @Test
    void shouldLoginSuccessfully() {

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);

        when(appUserRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(user));

        when(appUserRepository.save(any(AppUser.class)))
                .thenReturn(user);

        when(jwtService.generateAccessToken(any(AppUser.class)))
                .thenReturn("token");

        when(jwtService.getJwtExpirationMs())
                .thenReturn(3600000L);

        LoginResponse response =
                authService.login(loginRequest);

        assertEquals("token", response.getAccessToken());
    }

    @Test
    void shouldRejectInvalidCredentials() {

        when(authenticationManager.authenticate(any()))
                .thenThrow(new AuthenticationException("bad") {});

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest)
        );
    }

    @Test
    void shouldRejectInactiveUser() {

        user.setActive(false);

        when(authenticationManager.authenticate(any()))
                .thenReturn(null);

        when(appUserRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(user));

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest)
        );
    }

    @Test
    void shouldRejectMissingUser() {

        when(authenticationManager.authenticate(any()))
                .thenReturn(null);

        when(appUserRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest)
        );
    }

    @Test
    void shouldUpdateLastLoginTimestamp() {

        when(authenticationManager.authenticate(any()))
                .thenReturn(null);

        when(appUserRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(user));

        when(appUserRepository.save(any(AppUser.class)))
                .thenReturn(user);

        when(jwtService.generateAccessToken(any(AppUser.class)))
                .thenReturn("token");

        when(jwtService.getJwtExpirationMs())
                .thenReturn(3600000L);

        authService.login(loginRequest);

        ArgumentCaptor<AppUser> captor =
                ArgumentCaptor.forClass(AppUser.class);

        verify(appUserRepository).save(captor.capture());

        assertNotNull(captor.getValue().getLastLoginAt());
    }
}