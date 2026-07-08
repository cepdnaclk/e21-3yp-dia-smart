package com.diasmart.springapi.relationships.dto;

import com.diasmart.springapi.shared.enums.RelationshipType;
import java.time.OffsetDateTime;

public class RelationshipSummaryDto {

    private Long requestId;
    private Long userId;
    private String displayName;
    private String email;
    private Long patientId;
    private String patientName;
    private RelationshipType relationshipRole;
    private OffsetDateTime createdAt;

    public RelationshipSummaryDto() {
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public RelationshipType getRelationshipRole() {
        return relationshipRole;
    }

    public void setRelationshipRole(RelationshipType relationshipRole) {
        this.relationshipRole = relationshipRole;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
