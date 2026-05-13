package com.diasmart.springapi.patients.controller;

import com.diasmart.springapi.patients.dto.CreatePatientRequestDto;
import com.diasmart.springapi.patients.dto.PatientResponseDto;
import com.diasmart.springapi.patients.service.PatientService;

import com.diasmart.springapi.shared.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import jakarta.validation.Valid;


// Marks this class as REST API controller
@RestController

// Base URL for all patient APIs
@RequestMapping("/api/v1/patients")

// Auto-generates constructor
@RequiredArgsConstructor
public class PatientController {

    // Inject patient service
    private final PatientService patientService;


    // Create new patient
    @PostMapping
    public ApiResponse<PatientResponseDto>
    createPatient(

            // Read JSON request body
            @Valid
            @RequestBody
            CreatePatientRequestDto request
    ) {

        return ApiResponse.success(
                "Patient created successfully",

                patientService.createPatient(request)
        );
    }


    // Get patient by ID
    @GetMapping("/{patientId}")
    public ApiResponse<PatientResponseDto>
    getPatientById(

            // Extract patient ID from URL
            @PathVariable
            Long patientId
    ) {

        return ApiResponse.success(
                "Patient fetched successfully",

                patientService.getPatientById(patientId)
        );
    }


    // Get all patients
    @GetMapping
    public ApiResponse<List<PatientResponseDto>>
    getAllPatients() {

        return ApiResponse.success(
                "Patients fetched successfully",

                patientService.getAllPatients()
        );
    }

    // Update patient
    @PutMapping("/{patientId}")
    public ApiResponse<PatientResponseDto>
    updatePatient(

        @PathVariable
        Long patientId,

        @Valid
        @RequestBody
        CreatePatientRequestDto request
    ) {

    return ApiResponse.success(
            "Patient updated successfully",

            patientService.updatePatient(
                    patientId,
                    request
            )
    );
    }

    // Deactivate patient
    @DeleteMapping("/{patientId}")
    public ApiResponse<String>
    deactivatePatient(

        @PathVariable
        Long patientId
    ) {

    patientService.deactivatePatient(
            patientId
    );

    return ApiResponse.success(
            "Patient deactivated successfully",

            "Patient marked as inactive"
    );
    }
}