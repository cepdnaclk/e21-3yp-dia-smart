package com.diasmart.springapi.relationships.service;

import com.diasmart.springapi.relationships.entity.UserPatientAccess;
import com.diasmart.springapi.relationships.repository.UserPatientAccessRepository;
import com.diasmart.springapi.shared.enums.AccessStatus;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.entity.AppUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PatientAccessService is the ownership/relationship authorization layer.
 *
 * It prevents the wrong assumption:
 * currentUser.userId == patient.patientId
 *
 * Correct logic:
 * app_users.user_id -> user_patient_access -> patients.patient_id
 */
@Service
public class PatientAccessService {

    private final CurrentUserService currentUserService;
    private final UserPatientAccessRepository userPatientAccessRepository;

    public PatientAccessService(
            CurrentUserService currentUserService,
            UserPatientAccessRepository userPatientAccessRepository) {
        this.currentUserService = currentUserService;
        this.userPatientAccessRepository = userPatientAccessRepository;
    }

    @Transactional(readOnly = true)
    public boolean canViewPatient(Long patientId) {
        AppUser currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }

        return userPatientAccessRepository
                .existsByUserIdAndPatientIdAndStatusAndCanViewTrue(
                        currentUser.getUserId(),
                        patientId,
                        AccessStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public boolean canAcknowledgeAlerts(Long patientId) {
        AppUser currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }

        return userPatientAccessRepository
                .existsByUserIdAndPatientIdAndStatusAndCanAcknowledgeAlertsTrue(
                        currentUser.getUserId(),
                        patientId,
                        AccessStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public boolean canEditPrescriptions(Long patientId) {
        AppUser currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }

        return userPatientAccessRepository
                .existsByUserIdAndPatientIdAndStatusAndCanEditPrescriptionsTrue(
                        currentUser.getUserId(),
                        patientId,
                        AccessStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public void requireCanViewPatient(Long patientId) {
        if (!canViewPatient(patientId)) {
            throw new AccessDeniedException("You do not have access to this patient");
        }
    }

    @Transactional(readOnly = true)
    public void requireCanAcknowledgeAlerts(Long patientId) {
        if (!canAcknowledgeAlerts(patientId)) {
            throw new AccessDeniedException("You cannot acknowledge alerts for this patient");
        }
    }

    @Transactional(readOnly = true)
    public void requireCanEditPrescriptions(Long patientId) {
        if (!canEditPrescriptions(patientId)) {
            throw new AccessDeniedException("You cannot edit prescriptions for this patient");
        }
    }

    @Transactional(readOnly = true)
    public List<Long> getViewablePatientIdsForCurrentUser() {
        AppUser currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == UserRole.ADMIN) {
            throw new IllegalStateException(
                    "Admin-wide patient listing should be handled by a dedicated admin query");
        }

        return userPatientAccessRepository
                .findByUserIdAndStatusAndCanViewTrue(
                        currentUser.getUserId(),
                        AccessStatus.ACTIVE)
                .stream()
                .map(UserPatientAccess::getPatientId)
                .toList();
    }
}