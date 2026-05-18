package com.diasmart.springapi.relationships.dto;

import com.diasmart.springapi.shared.enums.AccessRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a user-patient access relationship.
 *
 * This creates a row in user_patient_access.
 */
public class CreatePatientAccessRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Access role is required")
    private AccessRole accessRole;

    @Size(max = 80, message = "Relationship label must not exceed 80 characters")
    private String relationshipLabel;

    private Boolean canView;

    private Boolean canAcknowledgeAlerts;

    private Boolean canEditPrescriptions;

    public CreatePatientAccessRequest() {
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

    public Boolean getCanView() {
        return canView;
    }

    public void setCanView(Boolean canView) {
        this.canView = canView;
    }

    public Boolean getCanAcknowledgeAlerts() {
        return canAcknowledgeAlerts;
    }

    public void setCanAcknowledgeAlerts(Boolean canAcknowledgeAlerts) {
        this.canAcknowledgeAlerts = canAcknowledgeAlerts;
    }

    public Boolean getCanEditPrescriptions() {
        return canEditPrescriptions;
    }

    public void setCanEditPrescriptions(Boolean canEditPrescriptions) {
        this.canEditPrescriptions = canEditPrescriptions;
    }
}