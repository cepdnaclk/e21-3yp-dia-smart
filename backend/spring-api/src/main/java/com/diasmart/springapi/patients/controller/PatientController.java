package com.diasmart.springapi.patients.controller;

import com.diasmart.springapi.common.responses.ApiResponse;
import com.diasmart.springapi.patients.dto.PatientProfileResponse;
import com.diasmart.springapi.patients.service.PatientService;
import com.diasmart.springapi.devices.service.DeviceService;
import com.diasmart.springapi.devices.dto.DeviceKitActivationResponseDTO;
import com.diasmart.springapi.devices.dto.PatientDeviceActivationRequestDTO;
import com.diasmart.springapi.devices.dto.PatientDeviceSummaryDTO;
import com.diasmart.springapi.shared.security.RequestIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;
    private final DeviceService deviceService;
    private final RequestIpResolver requestIpResolver;

    public PatientController(
            PatientService patientService,
            DeviceService deviceService,
            RequestIpResolver requestIpResolver) {
        this.patientService = patientService;
        this.deviceService = deviceService;
        this.requestIpResolver = requestIpResolver;
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<PatientProfileResponse>> getPatientProfile(
            @PathVariable Long patientId
    ) {

        PatientProfileResponse response =
                patientService.getPatientProfile(patientId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Patient profile retrieved successfully",
                        response
                )
        );
    }

    @GetMapping("/{patientId}/devices")
    public ResponseEntity<ApiResponse<List<PatientDeviceSummaryDTO>>> getPatientDevices(
            @PathVariable Long patientId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Patient devices retrieved successfully",
                        deviceService.getPatientDevices(patientId)
                )
        );
    }

    @PostMapping("/{patientId}/devices/activate-kit")
    public ResponseEntity<ApiResponse<DeviceKitActivationResponseDTO>> activateDeviceKit(
            @PathVariable Long patientId,
            @RequestBody PatientDeviceActivationRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        DeviceKitActivationResponseDTO response = deviceService.activateDeviceKit(
                patientId,
                request,
                requestIpResolver.resolve(httpRequest));

        return ResponseEntity.ok(
                ApiResponse.success(
                        activationMessage(response),
                        response
                )
        );
    }

    private String activationMessage(DeviceKitActivationResponseDTO response) {
        return "ALREADY_ACTIVE".equals(response.getActivationStatus())
                ? "Device kit is already active for this patient"
                : "Device kit activated successfully";
    }
}
