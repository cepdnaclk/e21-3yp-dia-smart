package com.diasmart.springapi.relationships.dto;

import com.diasmart.springapi.relationships.entity.UserPatientAccess;
import com.diasmart.springapi.shared.enums.AccessRole;
import com.diasmart.springapi.shared.enums.AccessStatus;

import java.time.OffsetDateTime;

/**
 * Safe response DTO for user-patient access relationships.
 */
public class PatientAccessResponse {

    private Long accessId;
    private Long userId;
    private Long patientId;
    private AccessRole accessRole;
    private String relationshipLabel;
    private boolean canView;
    private boolean canAcknowledgeAlerts;
    private boolean canEditPrescriptions;
    private OffsetDateTime createdAt;
    private AccessStatus status;
    private OffsetDateTime revokedAt;
    private Long revokedBy;

    public PatientAccessResponse() {
    }

    public static PatientAccessResponse fromEntity(UserPatientAccess access) {
        PatientAccessResponse response = new PatientAccessResponse();

        response.setAccessId(access.getAccessId());
        response.setUserId(access.getUserId());
        response.setPatientId(access.getPatientId());
        response.setAccessRole(access.getAccessRole());
        response.setRelationshipLabel(access.getRelationshipLabel());
        response.setCanView(access.isCanView());
        response.setCanAcknowledgeAlerts(access.isCanAcknowledgeAlerts());
        response.setCanEditPrescriptions(access.isCanEditPrescriptions());
        response.setCreatedAt(access.getCreatedAt());
        response.setStatus(access.getStatus());
        response.setRevokedAt(access.getRevokedAt());
        response.setRevokedBy(access.getRevokedBy());

        return response;
    }

    public Long getAccessId() {
        return accessId;
    }

    public void setAccessId(Long accessId) {
        this.accessId = accessId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public AccessRole getAccessRole() {
        return accessRole;
    }

    public void setAccessRole(AccessRole accessRole) {
        this.accessRole = accessRole;
    }

    public String getRelationshipLabel() {
        return relationshipLabel;
    }

    public void setRelationshipLabel(String relationshipLabel) {
        this.relationshipLabel = relationshipLabel;
    }

    public boolean isCanView() {
        return canView;
    }

    public void setCanView(boolean canView) {
        this.canView = canView;
    }

    public boolean isCanAcknowledgeAlerts() {
        return canAcknowledgeAlerts;
    }

    public void setCanAcknowledgeAlerts(boolean canAcknowledgeAlerts) {
        this.canAcknowledgeAlerts = canAcknowledgeAlerts;
    }

    public boolean isCanEditPrescriptions() {
        return canEditPrescriptions;
    }

    public void setCanEditPrescriptions(boolean canEditPrescriptions) {
        this.canEditPrescriptions = canEditPrescriptions;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public AccessStatus getStatus() {
        return status;
    }

    public void setStatus(AccessStatus status) {
        this.status = status;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public Long getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(Long revokedBy) {
        this.revokedBy = revokedBy;
    }
}