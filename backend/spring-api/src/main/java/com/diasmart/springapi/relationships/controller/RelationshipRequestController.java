package com.diasmart.springapi.relationships.controller;

import com.diasmart.springapi.relationships.dto.CreateRelationshipRequestDto;
import com.diasmart.springapi.relationships.dto.RelationshipRequestDto;
import com.diasmart.springapi.relationships.service.RelationshipRequestService;
import com.diasmart.springapi.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/relationship-requests")
public class RelationshipRequestController {

    private final RelationshipRequestService relationshipRequestService;

    public RelationshipRequestController(RelationshipRequestService relationshipRequestService) {
        this.relationshipRequestService = relationshipRequestService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RelationshipRequestDto>> sendRequest(
            @Valid @RequestBody CreateRelationshipRequestDto dto) {
        RelationshipRequestDto response = relationshipRequestService.sendRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Relationship request sent successfully", response));
    }

    @GetMapping("/incoming")
    public ResponseEntity<ApiResponse<List<RelationshipRequestDto>>> getIncomingRequests() {
        List<RelationshipRequestDto> response = relationshipRequestService.getIncomingRequests();
        return ResponseEntity.ok(ApiResponse.success("Incoming relationship requests retrieved successfully", response));
    }

    @GetMapping("/sent")
    public ResponseEntity<ApiResponse<List<RelationshipRequestDto>>> getSentRequests() {
        List<RelationshipRequestDto> response = relationshipRequestService.getSentRequests();
        return ResponseEntity.ok(ApiResponse.success("Sent relationship requests retrieved successfully", response));
    }

    @PatchMapping("/{requestId}/accept")
    public ResponseEntity<ApiResponse<RelationshipRequestDto>> acceptRequest(
            @PathVariable Long requestId) {
        RelationshipRequestDto response = relationshipRequestService.acceptRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success("Relationship request accepted successfully", response));
    }

    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<RelationshipRequestDto>> rejectRequest(
            @PathVariable Long requestId) {
        RelationshipRequestDto response = relationshipRequestService.rejectRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success("Relationship request rejected successfully", response));
    }

    @PatchMapping("/{requestId}/revoke")
    public ResponseEntity<ApiResponse<RelationshipRequestDto>> revokeRelationship(
            @PathVariable Long requestId) {
        RelationshipRequestDto response = relationshipRequestService.revokeRelationship(requestId);
        return ResponseEntity.ok(ApiResponse.success("Relationship revoked successfully", response));
    }
}
