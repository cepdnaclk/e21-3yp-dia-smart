package com.diasmart.springapi.users.controller;

import com.diasmart.springapi.shared.dto.ApiResponse;
import com.diasmart.springapi.users.dto.UserSettingsDto;
import com.diasmart.springapi.users.entity.UserSettings;
import com.diasmart.springapi.users.service.UserSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/settings")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    public UserSettingsController(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserSettingsDto>> getUserSettings() {
        UserSettings settings = userSettingsService.getUserSettings();
        UserSettingsDto dto = UserSettingsDto.fromEntity(settings);
        return ResponseEntity.ok(ApiResponse.success("User settings retrieved successfully", dto));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserSettingsDto>> updateUserSettings(
            @Valid @RequestBody UserSettingsDto dto) {
        UserSettings updated = userSettingsService.updateUserSettings(dto);
        UserSettingsDto responseDto = UserSettingsDto.fromEntity(updated);
        return ResponseEntity.ok(ApiResponse.success("User settings updated successfully", responseDto));
    }
}
