package com.diasmart.springapi.relationships.entity;

import com.diasmart.springapi.shared.enums.AccessRole;
import com.diasmart.springapi.shared.enums.AccessStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * UserPatientAccess maps to the final database table:
 * user_patient_access.
 *
 * This table is the bridge between:
 * - app_users.user_id
 * - patients.patient_id
 *
 * It should be used instead of directly comparing userId == patientId.
 */
@Entity
@Table(name = "user_patient_access")
public class UserPatientAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "access_id")
    private Long accessId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_role", nullable = false, length = 30)
    private AccessRole accessRole;

    @Column(name = "relationship_label", length = 80)
    private String relationshipLabel;

    @Column(name = "can_view", nullable = false)
    private boolean canView = true;

    @Column(name = "can_acknowledge_alerts", nullable = false)
    private boolean canAcknowledgeAlerts = false;

    @Column(name = "can_edit_prescriptions", nullable = false)
    private boolean canEditPrescriptions = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private AccessStatus status = AccessStatus.ACTIVE;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "revoked_by")
    private Long revokedBy;

    public UserPatientAccess() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }

        if (status == null) {
            status = AccessStatus.ACTIVE;
        }
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