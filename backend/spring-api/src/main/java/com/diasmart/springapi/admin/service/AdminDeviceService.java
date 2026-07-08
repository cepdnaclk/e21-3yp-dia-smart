package com.diasmart.springapi.admin.service;

import com.diasmart.springapi.devices.dto.DeviceKitRegistrationRequestDTO;
import com.diasmart.springapi.devices.dto.DeviceSummaryDTO;
import com.diasmart.springapi.devices.service.DeviceService;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.entity.AppUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminDeviceService {

    private final DeviceService deviceService;
    private final CurrentUserService currentUserService;

    public AdminDeviceService(
            DeviceService deviceService,
            CurrentUserService currentUserService) {
        this.deviceService = deviceService;
        this.currentUserService = currentUserService;
    }

    public List<DeviceSummaryDTO> getAllDevices() {
        requireAdmin();
        return deviceService.getAllDevices();
    }

    public void registerDeviceKit(DeviceKitRegistrationRequestDTO request) {
        requireAdmin();
        deviceService.registerDeviceKit(request);
    }

    private void requireAdmin() {
        AppUser currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only admins can access device management");
        }
    }
}
