package com.diasmart.springapi.relationships.controller;

import com.diasmart.springapi.relationships.dto.CreatePatientAccessRequest;
import com.diasmart.springapi.relationships.dto.PatientAccessResponse;
import com.diasmart.springapi.relationships.service.PatientAccessManagementService;
import com.diasmart.springapi.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PatientAccessController exposes APIs for managing user-patient relationships.
 *
 * Base path:
 * /api/v1/patient-access
 */
@RestController
@RequestMapping("/api/v1/patient-access")
public class PatientAccessController {

    private final PatientAccessManagementService patientAccessManagementService;

    public PatientAccessController(PatientAccessManagementService patientAccessManagementService) {
        this.patientAccessManagementService = patientAccessManagementService;
    }

    /**
     * Admin creates a relationship between an app user and patient profile.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PatientAccessResponse>> createAccess(
            @Valid @RequestBody CreatePatientAccessRequest request) {
        PatientAccessResponse response = patientAccessManagementService.createAccess(request);

        return ResponseEntity.ok(
                ApiResponse.success("Patient access created successfully", response));
    }

    /**
     * Current user lists their own active patient access relationships.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<PatientAccessResponse>>> getMyAccess() {
        List<PatientAccessResponse> response = patientAccessManagementService.getMyAccess();

        return ResponseEntity.ok(
                ApiResponse.success("Current user patient access retrieved successfully", response));
    }

    /**
     * Admin lists access records for a specific user.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<PatientAccessResponse>>> getAccessForUser(
            @PathVariable Long userId) {
        List<PatientAccessResponse> response = patientAccessManagementService.getAccessForUser(userId);

        return ResponseEntity.ok(
                ApiResponse.success("User patient access retrieved successfully", response));
    }

    /**
     * Admin revokes one access relationship.
     */
    @PatchMapping("/{accessId}/revoke")
    public ResponseEntity<ApiResponse<PatientAccessResponse>> revokeAccess(
            @PathVariable Long accessId) {
        PatientAccessResponse response = patientAccessManagementService.revokeAccess(accessId);

        return ResponseEntity.ok(
                ApiResponse.success("Patient access revoked successfully", response));
    }
}