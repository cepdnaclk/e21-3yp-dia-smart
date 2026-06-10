package com.diasmart.springapi.prescriptions.service;

import com.diasmart.springapi.prescriptions.dto.PrescriptionResponse;
import com.diasmart.springapi.prescriptions.entity.Prescription;
import com.diasmart.springapi.prescriptions.repository.PrescriptionRepository;
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.prescriptions.dto.CreatePrescriptionRequest;
import com.diasmart.springapi.users.entity.AppUser;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.diasmart.springapi.prescriptions.dto.UpdatePrescriptionRequest;
import com.diasmart.springapi.shared.exceptions.ResourceNotFoundException;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;

    private final AuthorizationService authorizationService;

    private final CurrentUserService currentUserService;

    public PrescriptionService(
            PrescriptionRepository prescriptionRepository,
            AuthorizationService authorizationService,
            CurrentUserService currentUserService
    ) {
        this.prescriptionRepository = prescriptionRepository;
        this.authorizationService = authorizationService;
        this.currentUserService = currentUserService;
    }

    public Page<PrescriptionResponse> getPrescriptions(
            Long patientId,
            Pageable pageable
    ) {

        authorizationService.authorize(
                Permission.READ_PATIENT_READINGS,
                patientId
        );

        return prescriptionRepository
                .findByPatientIdAndActiveTrue(
                        patientId,
                        pageable
                )
                .map(this::mapToResponse);
    }

    private PrescriptionResponse mapToResponse(
            Prescription prescription
    ) {

        PrescriptionResponse response =
                new PrescriptionResponse();

        response.setPrescriptionId(
                prescription.getPrescriptionId()
        );

        response.setInsulinProductId(
                prescription.getInsulinProductId()
        );

        response.setPrescriptionName(
                prescription.getPrescriptionName()
        );

        response.setStartDate(
                prescription.getStartDate()
        );

        response.setEndDate(
                prescription.getEndDate()
        );

        response.setActive(
                prescription.getActive()
        );

        response.setNotes(
                prescription.getNotes()
        );

        response.setCreatedAt(
                prescription.getCreatedAt()
        );

        return response;
    }

    public PrescriptionResponse createPrescription(
        Long patientId,
        CreatePrescriptionRequest request
    ) {

    authorizationService.authorize(
            Permission.WRITE_MANUAL_DOSE,
            patientId
    );

    AppUser currentUser =
            currentUserService.getCurrentUser();

    Prescription prescription =
            new Prescription();

    prescription.setPatientId(patientId);

    prescription.setInsulinProductId(
            request.getInsulinProductId()
    );

    prescription.setPrescribedByUserId(
            currentUser.getUserId()
    );

    prescription.setPrescriptionName(
            request.getPrescriptionName()
    );

    prescription.setStartDate(
            LocalDate.parse(request.getStartDate())
    );

    if (request.getEndDate() != null
            && !request.getEndDate().isBlank()) {

        prescription.setEndDate(
                LocalDate.parse(request.getEndDate())
        );
    }

    prescription.setActive(true);

    prescription.setNotes(
            request.getNotes()
    );

    prescription.setCreatedAt(
            OffsetDateTime.now()
    );

    prescription.setUpdatedAt(
            OffsetDateTime.now()
    );

    Prescription savedPrescription =
            prescriptionRepository.save(
                    prescription
            );

    return mapToResponse(savedPrescription);
}
    public PrescriptionResponse updatePrescription(
        Long prescriptionId,
        UpdatePrescriptionRequest request
) {

    Prescription prescription =
            prescriptionRepository.findById(
                    prescriptionId
            ).orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Prescription not found with id: "
                                    + prescriptionId
                    )
            );

    authorizationService.authorize(
            Permission.WRITE_MANUAL_DOSE,
            prescription.getPatientId()
    );

    if (request.getPrescriptionName() != null) {

        prescription.setPrescriptionName(
                request.getPrescriptionName()
        );
    }

    if (request.getStartDate() != null) {

        prescription.setStartDate(
                LocalDate.parse(
                        request.getStartDate()
                )
        );
    }

    if (request.getEndDate() != null) {

        prescription.setEndDate(
                LocalDate.parse(
                        request.getEndDate()
                )
        );
    }

    if (request.getActive() != null) {

        prescription.setActive(
                request.getActive()
        );
    }

    if (request.getNotes() != null) {

        prescription.setNotes(
                request.getNotes()
        );
    }

    prescription.setUpdatedAt(
            OffsetDateTime.now()
    );

    Prescription updatedPrescription =
            prescriptionRepository.save(
                    prescription
            );

    return mapToResponse(updatedPrescription);
}
    public void deactivatePrescription(
        Long prescriptionId
) {

    Prescription prescription =
            prescriptionRepository.findById(
                    prescriptionId
            ).orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Prescription not found with id: "
                                    + prescriptionId
                    )
            );

    authorizationService.authorize(
            Permission.WRITE_MANUAL_DOSE,
            prescription.getPatientId()
    );

    prescription.setActive(false);

    prescription.setUpdatedAt(
            OffsetDateTime.now()
    );

    prescriptionRepository.save(
            prescription
    );
}
}