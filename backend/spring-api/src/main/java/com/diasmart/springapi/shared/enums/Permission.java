package com.diasmart.springapi.shared.enums;

public enum Permission {

    READ_PATIENT_READINGS,

    WRITE_MANUAL_GLUCOSE,

    WRITE_MANUAL_DOSE,

    READ_STORAGE_HISTORY,

    READ_INVENTORY_HISTORY,

    READ_PATIENT_PROFILE,

    // Alert permissions
    READ_CLINICAL_ALERTS,

    ACKNOWLEDGE_CLINICAL_ALERTS,

    // Prescription permissions
    READ_PRESCRIPTION,

    CREATE_PRESCRIPTION,

    ARCHIVE_PRESCRIPTION,

    // Schedule permissions
    READ_SCHEDULE,

    MANAGE_SCHEDULE,

    // Analytics permissions
    READ_ADHERENCE_ANALYTICS,

    // Dashboard permissions
    READ_DASHBOARD,

    // Device setup permissions
    MANAGE_PATIENT_DEVICES,
}
