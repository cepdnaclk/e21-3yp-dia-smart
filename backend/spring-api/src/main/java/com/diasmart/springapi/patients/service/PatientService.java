package com.diasmart.springapi.patients.service;

import com.diasmart.springapi.patients.dto.PatientProfileResponse;
import com.diasmart.springapi.patients.entity.Patient;
import com.diasmart.springapi.patients.repository.PatientRepository;
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final AuthorizationService authorizationService;

    public PatientService(
            PatientRepository patientRepository,
            AuthorizationService authorizationService
    ) {
        this.patientRepository = patientRepository;
        this.authorizationService = authorizationService;
    }

    public PatientProfileResponse getPatientProfile(Long patientId) {

        authorizationService.authorize(
                Permission.READ_PATIENT_PROFILE,
                patientId
        );

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Patient not found with id: " + patientId
                        )
                );

        PatientProfileResponse response = new PatientProfileResponse();

        response.setPatientId(patient.getPatientId());
        response.setFullName(patient.getFullName());
        response.setDateOfBirth(patient.getDateOfBirth());
        response.setGender(patient.getGender());
        response.setDiabetesType(patient.getDiabetesType());
        response.setContactNumber(patient.getContactNumber());
        response.setEmergencyContactNumber(patient.getEmergencyContactNumber());

        return response;
    }
}