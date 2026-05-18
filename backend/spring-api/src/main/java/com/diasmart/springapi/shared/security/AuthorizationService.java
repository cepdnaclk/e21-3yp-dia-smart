package com.diasmart.springapi.shared.security;

import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.users.entity.AppUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private final CurrentUserService currentUserService;

    public AuthorizationService(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    public void authorize(Permission permission, Long patientId) {

        
        AppUser currentUser = currentUserService.getCurrentUser();

        UserRole role = currentUser.getRole();

        System.out.println("===== MANUAL ENTRY DEBUG =====");
        System.out.println("ROLE = " + role);
        System.out.println("CURRENT USER ID = " + currentUser.getUserId());
        System.out.println("PATIENT ID = " + patientId);


        // Admin cannot access clinical telemetry
        if (role == UserRole.ADMIN) {
            throw new AccessDeniedException(
                    "Admins cannot access patient clinical data"
            );
        }

        switch (permission) {

            case READ_PATIENT_PROFILE:
            case READ_PATIENT_READINGS:
            case READ_STORAGE_HISTORY:
            case READ_INVENTORY_HISTORY:
                validateReadAccess(currentUser, patientId);
                break;

            case WRITE_MANUAL_GLUCOSE:
            case WRITE_MANUAL_DOSE:
                validateManualEntryAccess(currentUser, patientId);
                break;
                

            default:
                throw new AccessDeniedException(
                        "Permission validation not implemented"
                );
        }
    }

    private void validateReadAccess(AppUser currentUser, Long patientId) {

        UserRole role = currentUser.getRole();

        // Patient can only access own data
        if (role == UserRole.PATIENT) {

            if (!currentUser.getUserId().equals(patientId)) {

                throw new AccessDeniedException(
                        "You cannot access another patient's data"
                );
            }
        }

        // TODO:
        // caregiver relationship validation
        // doctor relationship validation
    }

    private void validateManualEntryAccess(AppUser currentUser, Long patientId) {

        UserRole role = currentUser.getRole();

        // Patients can write only their own manual entries
        if (role == UserRole.PATIENT) {

            if (!currentUser.getUserId().equals(patientId)) {

                throw new AccessDeniedException(
                        "You cannot create entries for another patient"
                );
            }
        }

        // TODO:
        // caregiver relationship validation
    }
}
