package com.diasmart.springapi.inventory.repository;

import com.diasmart.springapi.inventory.entity.InventoryReading;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for inventory reading database operations.
 */
public interface InventoryReadingRepository
        extends JpaRepository<InventoryReading, Long> {

    /**
     * Retrieves paginated inventory history
     * ordered by latest reading first.
     */
    Page<InventoryReading>
    findByPatientIdOrderByMeasuredAtDesc(

            Long patientId,
            Pageable pageable
    );

    /**
     * Retrieves latest inventory reading
     * for a patient.
     */
    InventoryReading
    findTopByPatientIdOrderByMeasuredAtDesc(

            Long patientId
    );
}