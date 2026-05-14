package com.diasmart.springapi.storage.repository;

import com.diasmart.springapi.storage.entity.StorageReading;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for storage telemetry database operations.
 */
public interface StorageReadingRepository
        extends JpaRepository<StorageReading, Long> {

    /**
     * Retrieves paginated storage history
     * ordered by latest measured reading first.
     */
    Page<StorageReading>
    findByPatientIdOrderByMeasuredAtDesc(

            Long patientId,
            Pageable pageable
    );

    /**
     * Retrieves latest ingested storage reading.
     */
    StorageReading
    findTopByPatientIdOrderByCreatedAtDesc(

            Long patientId
    );

    /**
     * Retrieves latest measured storage reading.
     */
    StorageReading
    findTopByPatientIdOrderByMeasuredAtDesc(

            Long patientId
    );
}