package com.diasmart.springapi.devices.entity;

public enum DeviceActivationFailureCategory {
    INVALID_KIT,
    UNAUTHORIZED_PATIENT,
    DEVICE_CONFLICT,
    INACTIVE_DEVICE,
    TYPE_MISMATCH,
    RATE_LIMITED,
    INTEGRITY_ERROR
}
