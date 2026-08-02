package com.diasmart.springapi.relationships.controller;

import com.diasmart.springapi.relationships.dto.RelationshipSummaryDto;
import com.diasmart.springapi.relationships.service.RelationshipRequestService;
import com.diasmart.springapi.shared.dto.ApiResponse;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.users.dto.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/relationships")
public class RelationshipController {

    private final RelationshipRequestService relationshipRequestService;

    public RelationshipController(RelationshipRequestService relationshipRequestService) {
        this.relationshipRequestService = relationshipRequestService;
    }

    @GetMapping("/doctors")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getDoctors(
            @RequestParam(value = "q", required = false) String query) {
        List<UserResponse> doctors = relationshipRequestService.searchUsersByRole(UserRole.DOCTOR, query);
        return ResponseEntity.ok(ApiResponse.success("Doctors retrieved successfully", doctors));
    }

    @GetMapping("/caregivers")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getCaregivers(
            @RequestParam(value = "q", required = false) String query) {
        List<UserResponse> caregivers = relationshipRequestService.searchUsersByRole(UserRole.CAREGIVER, query);
        return ResponseEntity.ok(ApiResponse.success("Caregivers retrieved successfully", caregivers));
    }

    @GetMapping("/patients")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getPatients(
            @RequestParam(value = "q", required = false) String query) {
        List<UserResponse> patients = relationshipRequestService.searchUsersByRole(UserRole.PATIENT, query);
        return ResponseEntity.ok(ApiResponse.success("Patients retrieved successfully", patients));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<RelationshipSummaryDto>>> getMyRelationships() {
        List<RelationshipSummaryDto> response = relationshipRequestService.getMyRelationships();
        return ResponseEntity.ok(ApiResponse.success("Active relationships retrieved successfully", response));
    }

    @GetMapping("/my-patients")
    public ResponseEntity<ApiResponse<List<RelationshipSummaryDto>>> getMyPatients() {
        List<RelationshipSummaryDto> response = relationshipRequestService.getMyPatients();
        return ResponseEntity.ok(ApiResponse.success("Linked patients retrieved successfully", response));
    }
}
