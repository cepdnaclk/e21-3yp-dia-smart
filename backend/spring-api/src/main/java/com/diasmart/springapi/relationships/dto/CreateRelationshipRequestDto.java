package com.diasmart.springapi.relationships.dto;

import com.diasmart.springapi.shared.enums.RelationshipType;
import jakarta.validation.constraints.NotNull;

public class CreateRelationshipRequestDto {

    private Long targetUserId;

    private String targetEmail;

    private Long patientId;

    @NotNull(message = "Relationship role is required")
    private RelationshipType relationshipRole;

    private String message;

    public CreateRelationshipRequestDto() {
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getTargetEmail() {
        return targetEmail;
    }

    public void setTargetEmail(String targetEmail) {
        this.targetEmail = targetEmail;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public RelationshipType getRelationshipRole() {
        return relationshipRole;
    }

    public void setRelationshipRole(RelationshipType relationshipRole) {
        this.relationshipRole = relationshipRole;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
