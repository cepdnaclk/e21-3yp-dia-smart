package com.diasmart.springapi.deviceconfig.controller;

import com.diasmart.springapi.common.responses.ApiResponse;
import com.diasmart.springapi.deviceconfig.dto.CreateDeviceConfigurationRequestDTO;
import com.diasmart.springapi.deviceconfig.dto.DeviceConfigurationResponseDTO;
import com.diasmart.springapi.deviceconfig.dto.UpdateDeviceConfigurationRequestDTO;
import com.diasmart.springapi.deviceconfig.service.DeviceConfigurationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patient/device-configurations")
@PreAuthorize("hasAnyRole('PATIENT', 'CAREGIVER', 'DOCTOR', 'ADMIN')")
public class DeviceConfigurationController {

    private final DeviceConfigurationService configurationService;

    public DeviceConfigurationController(DeviceConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeviceConfigurationResponseDTO>> createConfiguration(
            @Valid @RequestBody CreateDeviceConfigurationRequestDTO dto) {

        DeviceConfigurationResponseDTO response = configurationService.createConfiguration(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Device configuration created and publishing initiated",
                                response
                        )
                );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeviceConfigurationResponseDTO>>> getConfigurations() {
        List<DeviceConfigurationResponseDTO> response = configurationService.getConfigurations();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Device configurations retrieved successfully",
                        response
                )
        );
    }

    @GetMapping("/{outerDeviceId}")
    public ResponseEntity<ApiResponse<DeviceConfigurationResponseDTO>> getConfiguration(
            @PathVariable Long outerDeviceId) {

        DeviceConfigurationResponseDTO response = configurationService.getConfiguration(outerDeviceId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Device configuration retrieved successfully",
                        response
                )
        );
    }

    @PutMapping("/{outerDeviceId}")
    public ResponseEntity<ApiResponse<DeviceConfigurationResponseDTO>> updateConfiguration(
            @PathVariable Long outerDeviceId,
            @Valid @RequestBody UpdateDeviceConfigurationRequestDTO dto) {

        DeviceConfigurationResponseDTO response = configurationService.updateConfiguration(outerDeviceId, dto);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Device configuration updated and publishing initiated",
                        response
                )
        );
    }

    @PostMapping("/{outerDeviceId}/send")
    public ResponseEntity<ApiResponse<DeviceConfigurationResponseDTO>> sendConfiguration(
            @PathVariable Long outerDeviceId) {

        DeviceConfigurationResponseDTO response = configurationService.sendConfiguration(outerDeviceId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Device configuration publishing initiated",
                        response
                )
        );
    }

    @GetMapping("/{outerDeviceId}/status")
    public ResponseEntity<ApiResponse<DeviceConfigurationResponseDTO>> getConfigurationStatus(
            @PathVariable Long outerDeviceId) {

        DeviceConfigurationResponseDTO response = configurationService.getConfigurationStatus(outerDeviceId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Device configuration status retrieved successfully",
                        response
                )
        );
    }
}
