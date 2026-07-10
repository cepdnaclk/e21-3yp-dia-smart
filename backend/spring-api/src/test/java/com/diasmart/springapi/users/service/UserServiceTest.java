package com.diasmart.springapi.users.service;

import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.dto.ChangePasswordRequest;
import com.diasmart.springapi.users.dto.UserResponse;
import com.diasmart.springapi.users.entity.AppUser;
import com.diasmart.springapi.users.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldReturnUserResponseAndPasswordChangedSuccessfully() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123!");

        AppUser user = new AppUser();
        user.setPasswordHash("hashedOldPassword");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("oldPassword123", "hashedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches("newPassword123!", "hashedOldPassword")).thenReturn(false);
        when(passwordEncoder.encode("newPassword123!")).thenReturn("hashedNewPassword");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserResponse response = userService.changeCurrentUserPassword(request);

        // Assert
        assertNotNull(response);
        assertEquals("hashedNewPassword", user.getPasswordHash());
        verify(appUserRepository).save(user);
        verify(passwordEncoder).encode("newPassword123!");
    }

    @Test
    void shouldThrowWhenCurrentPasswordIsIncorrect() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongOldPassword");
        request.setNewPassword("newPassword123!");

        AppUser user = new AppUser();
        user.setPasswordHash("hashedOldPassword");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("wrongOldPassword", "hashedOldPassword")).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.changeCurrentUserPassword(request);
        });

        assertEquals("Current password is incorrect", exception.getMessage());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenNewPasswordSameAsCurrent() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("oldPassword123");

        AppUser user = new AppUser();
        user.setPasswordHash("hashedOldPassword");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("oldPassword123", "hashedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches("oldPassword123", "hashedOldPassword")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.changeCurrentUserPassword(request);
        });

        assertEquals("New password must be different from current password", exception.getMessage());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCurrentPasswordIsNull() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(null);
        request.setNewPassword("newPassword123!");

        AppUser user = new AppUser();
        user.setPasswordHash("hashedOldPassword");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches(null, "hashedOldPassword"))
                .thenThrow(new IllegalArgumentException("Raw password cannot be null"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.changeCurrentUserPassword(request);
        });
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenNewPasswordIsNull() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword(null);

        AppUser user = new AppUser();
        user.setPasswordHash("hashedOldPassword");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("oldPassword123", "hashedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches(null, "hashedOldPassword"))
                .thenThrow(new IllegalArgumentException("Raw password cannot be null"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.changeCurrentUserPassword(request);
        });
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void shouldUpdatePasswordEvenWhenNewPasswordIsEmptyString() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("");

        AppUser user = new AppUser();
        user.setPasswordHash("hashedOldPassword");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("oldPassword123", "hashedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches("", "hashedOldPassword")).thenReturn(false);
        when(passwordEncoder.encode("")).thenReturn("hashedEmptyPassword");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserResponse response = userService.changeCurrentUserPassword(request);

        // Assert
        assertNotNull(response);
        assertEquals("hashedEmptyPassword", user.getPasswordHash());
        verify(appUserRepository).save(user);
    }

    @Test
    void shouldUpdatePasswordEvenWhenNewPasswordIsVeryLong() {
        // Arrange
        String longPassword = "a".repeat(100);
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword(longPassword);

        AppUser user = new AppUser();
        user.setPasswordHash("hashedOldPassword");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("oldPassword123", "hashedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches(longPassword, "hashedOldPassword")).thenReturn(false);
        when(passwordEncoder.encode(longPassword)).thenReturn("hashedLongPassword");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserResponse response = userService.changeCurrentUserPassword(request);

        // Assert
        assertNotNull(response);
        assertEquals("hashedLongPassword", user.getPasswordHash());
        verify(appUserRepository).save(user);
    }

    @Test
    void shouldThrowWhenUserNotFoundOrInvalidSession() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123!");

        when(currentUserService.getCurrentUser())
                .thenThrow(new IllegalStateException("No authenticated user found"));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            userService.changeCurrentUserPassword(request);
        });

        assertEquals("No authenticated user found", exception.getMessage());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUserIsNull() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123!");

        when(currentUserService.getCurrentUser()).thenReturn(null);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            userService.changeCurrentUserPassword(request);
        });
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenPasswordEncoderFails() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123!");

        AppUser user = new AppUser();
        user.setPasswordHash("hashedOldPassword");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("oldPassword123", "hashedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches("newPassword123!", "hashedOldPassword")).thenReturn(false);
        when(passwordEncoder.encode("newPassword123!"))
                .thenThrow(new RuntimeException("Encoder service unavailable"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.changeCurrentUserPassword(request);
        });

        assertEquals("Encoder service unavailable", exception.getMessage());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenRepositoryFailsToSave() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123!");

        AppUser user = new AppUser();
        user.setPasswordHash("hashedOldPassword");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("oldPassword123", "hashedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches("newPassword123!", "hashedOldPassword")).thenReturn(false);
        when(passwordEncoder.encode("newPassword123!")).thenReturn("hashedNewPassword");
        when(appUserRepository.save(any(AppUser.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.changeCurrentUserPassword(request);
        });

        assertEquals("Database error", exception.getMessage());
    }

    @Test
    void shouldThrowWhenChangePasswordRequestIsNull() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(new AppUser());

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            userService.changeCurrentUserPassword(null);
        });
        verify(appUserRepository, never()).save(any());
    }
}
