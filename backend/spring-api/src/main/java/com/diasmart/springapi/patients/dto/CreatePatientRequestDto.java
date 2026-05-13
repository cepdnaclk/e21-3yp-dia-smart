package com.diasmart.springapi.patients.dto;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;


// DTO received from frontend
@Getter
@Setter
public class CreatePatientRequestDto {

    // NIC number
    @NotBlank(message = "NIC is required")
    private String nic;


    // Full name
    @NotBlank(message = "Full name is required")
    private String fullName;


    // Date of birth
    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;


    // Gender
    @NotBlank(message = "Gender is required")
    private String gender;


    // Contact number
    @NotBlank(message = "Contact number is required")

    @Pattern(
            regexp = "^(07)[0-9]{8}$",
            message = "Invalid Sri Lankan phone number"
    )
    private String contactNumber;


    // Emergency contact number
    @Pattern(
            regexp = "^(07)[0-9]{8}$",
            message = "Invalid emergency contact number"
    )
    private String emergencyContactNumber;


    // Address
    @NotBlank(message = "Address is required")
    private String address;


    // Diabetes type
    @NotBlank(message = "Diabetes type is required")
    private String diabetesType;


    // Minimum target glucose
    @NotNull(message = "Minimum glucose target required")
    private Double targetGlucoseMinMgDl;


    // Maximum target glucose
    @NotNull(message = "Maximum glucose target required")
    private Double targetGlucoseMaxMgDl;


    // Additional notes
    private String notes;
}