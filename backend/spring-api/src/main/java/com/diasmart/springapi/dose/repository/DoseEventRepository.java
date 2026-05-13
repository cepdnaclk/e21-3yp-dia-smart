package com.diasmart.springapi.dose.repository;

import com.diasmart.springapi.dose.entity.DoseEvent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;


// Repository for dose_events table operations
public interface DoseEventRepository
        extends JpaRepository<DoseEvent, Long> {

    // Fetch patient's dose history ordered by latest first
    Page<DoseEvent> findByPatientIdOrderByInjectedAtDesc(
            Long patientId,
            Pageable pageable
    );
}
