package com.diasmart.springapi.shared.enums;

/**
 * AccessRole represents the relationship between an app user
 * and a patient medical profile.
 *
 * This maps to:
 * user_patient_access.access_role
 */
public enum AccessRole {
    SELF,
    CAREGIVER,
    DOCTOR
}