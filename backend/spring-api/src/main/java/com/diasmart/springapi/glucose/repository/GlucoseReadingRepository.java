package com.diasmart.springapi.glucose.repository;

import com.diasmart.springapi.glucose.entity.GlucoseReading;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for glucose reading database operations.
 */
public interface GlucoseReadingRepository
        extends JpaRepository<GlucoseReading, Long> {

    /**
     * Retrieves glucose history for a patient
     * ordered by latest reading first.
     */
    Page<GlucoseReading>
    findByPatientIdOrderByMeasuredAtDesc(

            Long patientId,
            Pageable pageable
    );
}