package com.diasmart.springapi.patients.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;


// Maps this class to patients table
@Entity

// Database table name
@Table(name = "patients")

@Getter
@Setter
public class Patient {

    // Primary key
    @Id

    // Auto-generated ID
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    // Maps patient_id column
    @Column(name = "patient_id")
    private Long patientId;


    // Unique public UUID
    @Column(name = "patient_uuid")
    private UUID patientUuid;


    // National identity card number
    @Column(name = "nic")
    private String nic;


    // Patient full name
    @Column(name = "full_name")
    private String fullName;


    // Date of birth
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;


    // Gender
    @Column(name = "gender")
    private String gender;


    // Contact number
    @Column(name = "contact_number")
    private String contactNumber;


    // Emergency contact number
    @Column(name = "emergency_contact_number")
    private String emergencyContactNumber;


    // Patient address
    @Column(name = "address")
    private String address;


    // Diabetes type
    @Column(name = "diabetes_type")
    private String diabetesType;


    // Minimum target glucose
    @Column(name = "target_glucose_min_mg_dl")
    private Double targetGlucoseMinMgDl;


    // Maximum target glucose
    @Column(name = "target_glucose_max_mg_dl")
    private Double targetGlucoseMaxMgDl;


    // Whether patient account is active
    @Column(name = "is_active")
    private Boolean isActive;


    // Additional notes
    @Column(name = "notes")
    private String notes;


    // Record creation timestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;


    // Record update timestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}