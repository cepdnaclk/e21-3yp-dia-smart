package com.diasmart.springapi.patients.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;


// DTO sent to frontend
@Getter
@Builder
@AllArgsConstructor
public class PatientResponseDto {

    // Patient database ID
    private Long patientId;

    // Public UUID
    private UUID patientUuid;

    // NIC number
    private String nic;

    // Full name
    private String fullName;

    // Date of birth
    private LocalDate dateOfBirth;

    // Gender
    private String gender;

    // Contact number
    private String contactNumber;

    // Emergency contact number
    private String emergencyContactNumber;

    // Address
    private String address;

    // Diabetes type
    private String diabetesType;

    // Target glucose minimum
    private Double targetGlucoseMinMgDl;

    // Target glucose maximum
    private Double targetGlucoseMaxMgDl;

    // Account active status
    private Boolean isActive;

    // Additional notes
    private String notes;
}