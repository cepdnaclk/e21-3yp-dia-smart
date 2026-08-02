package com.diasmart.springapi.patients.controller;

import com.diasmart.springapi.common.responses.ApiResponse;
import com.diasmart.springapi.patients.dto.PatientProfileResponse;
import com.diasmart.springapi.patients.service.PatientService;
import com.diasmart.springapi.devices.service.DeviceService;
import com.diasmart.springapi.devices.dto.PatientDeviceActivationRequestDTO;
import com.diasmart.springapi.devices.dto.PatientDeviceSummaryDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;
    private final DeviceService deviceService;

    public PatientController(PatientService patientService, DeviceService deviceService) {
        this.patientService = patientService;
        this.deviceService = deviceService;
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
    public ResponseEntity<ApiResponse<Void>> activateDeviceKit(
            @PathVariable Long patientId,
            @RequestBody PatientDeviceActivationRequestDTO request
    ) {
        deviceService.activateDeviceKit(patientId, request);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Device kit activated successfully",
                        null
                )
        );
    }
}
