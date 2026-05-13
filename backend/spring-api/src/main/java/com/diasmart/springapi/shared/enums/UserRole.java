package com.diasmart.springapi.shared.enums;

/**
 * UserRole defines the main system roles used in Dia-Smart RBAC.
 *
 * PATIENT:
 * The elderly diabetic user whose healthcare data is monitored.
 *
 * CAREGIVER:
 * A family member or support person who monitors linked patients.
 *
 * DOCTOR:
 * A healthcare professional who can view linked patient data and manage
 * clinical items later.
 *
 * ADMIN:
 * A technical system administrator.
 */
public enum UserRole {
    PATIENT,
    CAREGIVER,
    DOCTOR,
    ADMIN
}