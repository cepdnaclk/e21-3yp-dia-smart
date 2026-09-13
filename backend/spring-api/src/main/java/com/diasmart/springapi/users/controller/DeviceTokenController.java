package com.diasmart.springapi.users.controller;

import com.diasmart.springapi.shared.dto.ApiResponse;
import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.dto.RegisterDeviceTokenRequest;
import com.diasmart.springapi.users.entity.UserDeviceToken;
import com.diasmart.springapi.users.service.DeviceTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/device-token")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;
    private final CurrentUserService currentUserService;

    public DeviceTokenController(
            DeviceTokenService deviceTokenService,
            CurrentUserService currentUserService
    ) {
        this.deviceTokenService = deviceTokenService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDeviceToken>> registerDeviceToken(
            @Valid @RequestBody RegisterDeviceTokenRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUserId();
        UserDeviceToken token = deviceTokenService.registerToken(currentUserId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Device token registered successfully", token)
        );
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> unregisterDeviceToken(
            @RequestParam String token
    ) {
        Long currentUserId = currentUserService.getCurrentUserId();
        deviceTokenService.unregisterToken(currentUserId, token);
        return ResponseEntity.ok(
                ApiResponse.success("Device token unregistered successfully", null)
        );
    }
}
