package com.diasmart.springapi.shared.security;

import com.diasmart.springapi.relationships.service.PatientAccessService;
import com.diasmart.springapi.shared.enums.Permission;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private final PatientAccessService patientAccessService;

    public AuthorizationService(PatientAccessService patientAccessService) {
        this.patientAccessService = patientAccessService;
    }

    public void authorize(Permission permission, Long patientId) {
        switch (permission) {

            case READ_PATIENT_PROFILE:
            case READ_PATIENT_READINGS:
            case READ_STORAGE_HISTORY:
            case READ_INVENTORY_HISTORY:
            case READ_CLINICAL_ALERTS:
            case READ_PRESCRIPTION:
            case READ_SCHEDULE:
            case READ_ADHERENCE_ANALYTICS:
            case READ_DASHBOARD:
                patientAccessService.requireCanViewPatient(patientId);
                break;

            case WRITE_MANUAL_GLUCOSE:
            case WRITE_MANUAL_DOSE:
                /*
                 * The current final schema has can_view, can_acknowledge_alerts,
                 * and can_edit_prescriptions.
                 *
                 * It does not yet have a separate can_create_manual_entries flag.
                 * For now, manual glucose/dose entry requires patient visibility.
                 *
                 * If the team wants stricter control later, add a dedicated
                 * permission column in user_patient_access.
                 */
                patientAccessService.requireCanViewPatient(patientId);
                break;

            case ACKNOWLEDGE_CLINICAL_ALERTS:
                patientAccessService.requireCanAcknowledgeAlerts(patientId);
                break;

            case CREATE_PRESCRIPTION:
            case ARCHIVE_PRESCRIPTION:
            case MANAGE_SCHEDULE:
                patientAccessService.requireCanEditPrescriptions(patientId);
                break;

            case MANAGE_PATIENT_DEVICES:
                patientAccessService.requireCanManagePatientDevices(patientId);
                break;

            default:
                throw new org.springframework.security.access.AccessDeniedException(
                        "Permission validation not implemented for: " + permission);
        }
    }
}
