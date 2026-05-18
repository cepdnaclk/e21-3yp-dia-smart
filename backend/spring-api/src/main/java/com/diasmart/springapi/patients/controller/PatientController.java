package com.diasmart.springapi.patients.controller;

import com.diasmart.springapi.common.responses.ApiResponse;
import com.diasmart.springapi.patients.dto.PatientProfileResponse;
import com.diasmart.springapi.patients.service.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
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
}