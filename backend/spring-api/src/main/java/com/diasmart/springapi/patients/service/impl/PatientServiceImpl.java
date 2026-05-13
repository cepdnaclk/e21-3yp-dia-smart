package com.diasmart.springapi.patients.service.impl;

import com.diasmart.springapi.patients.dto.CreatePatientRequestDto;
import com.diasmart.springapi.patients.dto.PatientResponseDto;
import com.diasmart.springapi.patients.entity.Patient;
import com.diasmart.springapi.patients.repository.PatientRepository;
import com.diasmart.springapi.patients.service.PatientService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import java.time.OffsetDateTime;



@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    // Inject repository automatically
    private final PatientRepository patientRepository;


    // Create new patient
    @Override
    public PatientResponseDto createPatient(
            CreatePatientRequestDto request
    ) {

        // Create entity object
        Patient patient = new Patient();

        // Generate UUID
        patient.setPatientUuid(UUID.randomUUID());

        // Set values from request DTO
        patient.setNic(request.getNic());
        patient.setFullName(request.getFullName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setContactNumber(request.getContactNumber());
        patient.setEmergencyContactNumber(
                request.getEmergencyContactNumber()
        );
        patient.setAddress(request.getAddress());
        patient.setDiabetesType(request.getDiabetesType());
        patient.setTargetGlucoseMinMgDl(
                request.getTargetGlucoseMinMgDl()
        );
        patient.setTargetGlucoseMaxMgDl(
                request.getTargetGlucoseMaxMgDl()
        );
        patient.setNotes(request.getNotes());

        // Default active status
        patient.setIsActive(true);
        //adding timestamps
        patient.setCreatedAt(OffsetDateTime.now());
        patient.setUpdatedAt(OffsetDateTime.now());

        // Save to database
        Patient savedPatient =
                patientRepository.save(patient);

        // Convert entity -> DTO
        return mapToDto(savedPatient);
    }


    // Get patient using ID
    @Override
    public PatientResponseDto getPatientById(
            Long patientId
    ) {

        Patient patient = patientRepository
                .findById(patientId)
                .orElseThrow(() ->
                        new RuntimeException("Patient not found")
                );

        return mapToDto(patient);
    }


    // Get all patients
    @Override
    public List<PatientResponseDto> getAllPatients() {

        return patientRepository
                .findAll()
                .stream()
                .map(this::mapToDto)
                .toList();
    }


    // Convert Entity -> DTO
    private PatientResponseDto mapToDto(
            Patient patient
    ) {

        return PatientResponseDto.builder()
                .patientId(patient.getPatientId())
                .patientUuid(patient.getPatientUuid())
                .nic(patient.getNic())
                .fullName(patient.getFullName())
                .dateOfBirth(patient.getDateOfBirth())
                .gender(patient.getGender())
                .contactNumber(patient.getContactNumber())
                .emergencyContactNumber(
                        patient.getEmergencyContactNumber()
                )
                .address(patient.getAddress())
                .diabetesType(patient.getDiabetesType())
                .targetGlucoseMinMgDl(
                        patient.getTargetGlucoseMinMgDl()
                )
                .targetGlucoseMaxMgDl(
                        patient.getTargetGlucoseMaxMgDl()
                )
                .isActive(patient.getIsActive())
                .notes(patient.getNotes())
                .build();
    }

    // Update patient details
    @Override
    public PatientResponseDto updatePatient(

        Long patientId,

        CreatePatientRequestDto request
    ) {

    // Find existing patient
    Patient patient = patientRepository
            .findById(patientId)
            .orElseThrow(() ->
                    new RuntimeException(
                            "Patient not found"
                    )
            );

    // Update fields
    patient.setNic(request.getNic());

    patient.setFullName(request.getFullName());

    patient.setDateOfBirth(
            request.getDateOfBirth()
    );

    patient.setGender(request.getGender());

    patient.setContactNumber(
            request.getContactNumber()
    );

    patient.setEmergencyContactNumber(
            request.getEmergencyContactNumber()
    );

    patient.setAddress(request.getAddress());

    patient.setDiabetesType(
            request.getDiabetesType()
    );

    patient.setTargetGlucoseMinMgDl(
            request.getTargetGlucoseMinMgDl()
    );

    patient.setTargetGlucoseMaxMgDl(
            request.getTargetGlucoseMaxMgDl()
    );

    patient.setNotes(request.getNotes());

    // Update timestamp
    patient.setUpdatedAt(
            OffsetDateTime.now()
    );

    // Save updated entity
    Patient updatedPatient =
            patientRepository.save(patient);

    // Convert entity -> DTO
    return mapToDto(updatedPatient);
    }

    // Soft delete patient
    @Override
    public void deactivatePatient(
        Long patientId
    ) {

    // Find patient
    Patient patient = patientRepository
            .findById(patientId)
            .orElseThrow(() ->
                    new RuntimeException(
                            "Patient not found"
                    )
            );

    // Mark inactive instead of deleting
    patient.setIsActive(false);

    // Update timestamp
    patient.setUpdatedAt(
            OffsetDateTime.now()
    );

    // Save changes
    patientRepository.save(patient);
}
}