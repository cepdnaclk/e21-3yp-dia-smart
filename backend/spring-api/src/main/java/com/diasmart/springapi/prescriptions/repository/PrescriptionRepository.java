package com.diasmart.springapi.prescriptions.repository;

import com.diasmart.springapi.prescriptions.entity.Prescription;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionRepository
        extends JpaRepository<Prescription, Long> {

    Page<Prescription>
    findByPatientIdAndActiveTrue(
            Long patientId,
            Pageable pageable
    );
}