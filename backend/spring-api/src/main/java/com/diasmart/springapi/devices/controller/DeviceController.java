package com.diasmart.springapi.devices.controller;

import com.diasmart.springapi.common.responses.ApiResponse;
import com.diasmart.springapi.devices.dto.AssignDeviceRequestDTO;
import com.diasmart.springapi.devices.dto.DeviceDiagnosticsDTO;
import com.diasmart.springapi.devices.dto.DeviceResponseDTO;
import com.diasmart.springapi.devices.dto.DeviceSummaryDTO;
import com.diasmart.springapi.devices.dto.RegisterDeviceRequestDTO;
import com.diasmart.springapi.devices.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

        private final DeviceService deviceService;

        public DeviceController(DeviceService deviceService) {
                this.deviceService = deviceService;
        }

        @GetMapping
        public ResponseEntity<ApiResponse<List<DeviceSummaryDTO>>> getAllDevices() {

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Devices retrieved successfully",
                                                deviceService.getAllDevices()));
        }

        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<DeviceResponseDTO>> getDeviceById(
                        @PathVariable Long id) {

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Device retrieved successfully",
                                                deviceService.getDeviceById(id)));
        }

        @PostMapping
        public ResponseEntity<ApiResponse<DeviceResponseDTO>> registerDevice(
                        @Valid @RequestBody RegisterDeviceRequestDTO dto) {

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(
                                                ApiResponse.success(
                                                                "Device registered successfully",
                                                                deviceService.registerDevice(dto)));
        }

        @PatchMapping("/{id}/assign")
        public ResponseEntity<ApiResponse<DeviceResponseDTO>> assignDevice(
                        @PathVariable Long id,
                        @Valid @RequestBody AssignDeviceRequestDTO dto) {

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Device assigned successfully",
                                                deviceService.assignDevice(id, dto)));
        }

        @DeleteMapping("/{id}/assign")
        public ResponseEntity<ApiResponse<DeviceResponseDTO>> unassignDevice(
                        @PathVariable Long id) {

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Device unassigned successfully",
                                                deviceService.unassignDevice(id)));
        }

        @GetMapping("/{id}/diagnostics")
        public ResponseEntity<ApiResponse<DeviceDiagnosticsDTO>> getDeviceDiagnostics(
                        @PathVariable Long id) {

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Device diagnostics retrieved successfully",
                                                deviceService.getDeviceDiagnostics(id)));
        }
}
