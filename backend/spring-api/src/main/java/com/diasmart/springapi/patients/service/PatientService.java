package com.diasmart.springapi.patients.service;

import com.diasmart.springapi.patients.dto.CreatePatientRequestDto;
import com.diasmart.springapi.patients.dto.PatientResponseDto;

import java.util.List;


public interface PatientService {

    // Create new patient
    PatientResponseDto createPatient(
            CreatePatientRequestDto request
    );

    // Get single patient by ID
    PatientResponseDto getPatientById(
            Long patientId
    );

    // Get all patients
    List<PatientResponseDto> getAllPatients();

    // Update existing patient
    PatientResponseDto updatePatient(

                Long patientId,
                //(Later in production: UpdatePatientRequestDto could be separate.)
                CreatePatientRequestDto request
  );


    // Deactivate patient
    void deactivatePatient(
        Long patientId
    );
}