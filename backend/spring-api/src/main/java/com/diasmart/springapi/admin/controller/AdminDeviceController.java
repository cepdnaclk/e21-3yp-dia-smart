package com.diasmart.springapi.admin.controller;

import com.diasmart.springapi.admin.service.AdminDeviceService;
import com.diasmart.springapi.devices.dto.DeviceKitDTO;
import com.diasmart.springapi.devices.dto.DeviceKitRegistrationRequestDTO;
import com.diasmart.springapi.devices.dto.DeviceSummaryDTO;
import com.diasmart.springapi.devices.dto.BuyerDeviceKitsDTO;
import com.diasmart.springapi.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/devices")
public class AdminDeviceController {

    private final AdminDeviceService adminDeviceService;

    public AdminDeviceController(AdminDeviceService adminDeviceService) {
        this.adminDeviceService = adminDeviceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeviceSummaryDTO>>> getAllDevices() {
        List<DeviceSummaryDTO> response = adminDeviceService.getAllDevices();
        return ResponseEntity.ok(
                ApiResponse.success("Devices retrieved successfully", response));
    }

    @GetMapping("/device-kits")
    public ResponseEntity<ApiResponse<List<BuyerDeviceKitsDTO>>> getDeviceKits() {
        List<BuyerDeviceKitsDTO> response = adminDeviceService.getDeviceKits();
        return ResponseEntity.ok(
                ApiResponse.success("Device kits retrieved successfully", response));
    }

    @PostMapping("/register-kit")
    public ResponseEntity<ApiResponse<DeviceKitDTO>> registerKit(
            @Valid @RequestBody DeviceKitRegistrationRequestDTO request) {
        DeviceKitDTO response = adminDeviceService.registerDeviceKit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Device kit registered successfully", response));
    }
}
