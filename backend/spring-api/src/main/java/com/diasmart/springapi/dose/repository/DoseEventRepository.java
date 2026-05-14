package com.diasmart.springapi.dose.repository;

import com.diasmart.springapi.dose.entity.DoseEvent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for dose event database operations.
 */
public interface DoseEventRepository
        extends JpaRepository<DoseEvent, Long> {

    /**
     * Retrieves dose history for a patient
     * ordered by latest injection first.
     */
    Page<DoseEvent> findByPatientIdOrderByInjectedAtDesc(
            Long patientId,
            Pageable pageable
    );
}