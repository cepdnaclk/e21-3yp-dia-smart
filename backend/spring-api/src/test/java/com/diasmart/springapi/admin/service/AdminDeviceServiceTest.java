package com.diasmart.springapi.admin.service;

import com.diasmart.springapi.devices.dto.DeviceKitDTO;
import com.diasmart.springapi.devices.dto.DeviceKitRegistrationRequestDTO;
import com.diasmart.springapi.devices.service.DeviceService;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDeviceServiceTest {

    @Mock
    private DeviceService deviceService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AdminDeviceService adminDeviceService;

    @Test
    void registerDeviceKitShouldRejectNonAdminUsers() {
        when(currentUserService.getCurrentUser())
                .thenReturn(user(UserRole.CAREGIVER));

        DeviceKitRegistrationRequestDTO request = new DeviceKitRegistrationRequestDTO();

        assertThrows(
                AccessDeniedException.class,
                () -> adminDeviceService.registerDeviceKit(request));

        verify(deviceService, never()).registerDeviceKit(request);
    }

    @Test
    void registerDeviceKitShouldReturnCreatedKitForAdminUsers() {
        when(currentUserService.getCurrentUser())
                .thenReturn(user(UserRole.ADMIN));

        DeviceKitRegistrationRequestDTO request = new DeviceKitRegistrationRequestDTO();
        DeviceKitDTO expected = new DeviceKitDTO();

        when(deviceService.registerDeviceKit(request))
                .thenReturn(expected);

        DeviceKitDTO actual = adminDeviceService.registerDeviceKit(request);

        assertSame(expected, actual);
        verify(deviceService).registerDeviceKit(request);
    }

    private AppUser user(UserRole role) {
        AppUser user = new AppUser();
        user.setRole(role);
        return user;
    }
}
