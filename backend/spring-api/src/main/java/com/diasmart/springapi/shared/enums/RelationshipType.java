package com.diasmart.springapi.shared.enums;

/**
 * RelationshipType defines the type of patient-linked access.
 *
 * CAREGIVER:
 * A caregiver is linked to a patient for daily monitoring and support.
 *
 * DOCTOR:
 * A doctor is linked to a patient for clinical visibility.
 */
public enum RelationshipType {
    CAREGIVER,
    DOCTOR
}