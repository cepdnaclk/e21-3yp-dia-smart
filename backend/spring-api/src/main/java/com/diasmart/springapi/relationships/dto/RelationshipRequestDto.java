package com.diasmart.springapi.relationships.dto;

import com.diasmart.springapi.relationships.entity.RelationshipRequest;
import com.diasmart.springapi.shared.enums.RelationshipStatus;
import com.diasmart.springapi.shared.enums.RelationshipType;
import java.time.OffsetDateTime;

public class RelationshipRequestDto {

    private Long requestId;
    private Long requesterUserId;
    private String requesterName;
    private Long targetUserId;
    private String targetName;
    private Long patientId;
    private String patientName;
    private RelationshipType relationshipRole;
    private RelationshipStatus status;
    private String message;
    private OffsetDateTime createdAt;
    private OffsetDateTime respondedAt;

    public RelationshipRequestDto() {
    }

    public static RelationshipRequestDto fromEntity(
            RelationshipRequest entity,
            String requesterName,
            String targetName,
            String patientName) {
        RelationshipRequestDto dto = new RelationshipRequestDto();
        dto.setRequestId(entity.getRequestId());
        dto.setRequesterUserId(entity.getRequesterUserId());
        dto.setRequesterName(requesterName);
        dto.setTargetUserId(entity.getTargetUserId());
        dto.setTargetName(targetName);
        dto.setPatientId(entity.getPatientId());
        dto.setPatientName(patientName);
        dto.setRelationshipRole(entity.getRelationshipRole());
        dto.setStatus(entity.getStatus());
        dto.setMessage(entity.getMessage());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setRespondedAt(entity.getRespondedAt());
        return dto;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Long getRequesterUserId() {
        return requesterUserId;
    }

    public void setRequesterUserId(Long requesterUserId) {
        this.requesterUserId = requesterUserId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
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

    public RelationshipStatus getStatus() {
        return status;
    }

    public void setStatus(RelationshipStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(OffsetDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }
}
