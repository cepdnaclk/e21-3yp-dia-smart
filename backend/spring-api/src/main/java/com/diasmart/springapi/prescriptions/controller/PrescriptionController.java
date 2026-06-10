package com.diasmart.springapi.prescriptions.controller;

import com.diasmart.springapi.prescriptions.dto.PrescriptionResponse;
import com.diasmart.springapi.prescriptions.service.PrescriptionService;
import com.diasmart.springapi.shared.dto.ApiResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.diasmart.springapi.prescriptions.dto.CreatePrescriptionRequest;
import com.diasmart.springapi.prescriptions.dto.UpdatePrescriptionRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(
            PrescriptionService prescriptionService
    ) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping("/patients/{patientId}/prescriptions")
    public ResponseEntity<ApiResponse<Page<PrescriptionResponse>>>
    getPrescriptions(

            @PathVariable Long patientId,

            @PageableDefault(size = 20)
            Pageable pageable
    ) {

        Page<PrescriptionResponse> response =
                prescriptionService.getPrescriptions(
                        patientId,
                        pageable
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Prescriptions retrieved successfully",
                        response
                )
        );
    }

    @PostMapping("/patients/{patientId}/prescriptions")
    public ResponseEntity<ApiResponse<PrescriptionResponse>>
    createPrescription(

            @PathVariable Long patientId,

            @Valid @RequestBody
            CreatePrescriptionRequest request
    ) {

        PrescriptionResponse response =
                prescriptionService.createPrescription(
                        patientId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Prescription created successfully",
                        response
                )
        );
    }

    @PatchMapping("/prescriptions/{prescriptionId}")
    public ResponseEntity<ApiResponse<PrescriptionResponse>>
    updatePrescription(

            @PathVariable Long prescriptionId,

            @RequestBody
            UpdatePrescriptionRequest request
    ) {

        PrescriptionResponse response =
                prescriptionService.updatePrescription(
                        prescriptionId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Prescription updated successfully",
                        response
                )
        );
    }

    @DeleteMapping("/prescriptions/{prescriptionId}")
    public ResponseEntity<ApiResponse<Void>>
    deactivatePrescription(

            @PathVariable Long prescriptionId
    ) {

        prescriptionService.deactivatePrescription(
                prescriptionId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Prescription deactivated successfully"
                )
        );
    }
}