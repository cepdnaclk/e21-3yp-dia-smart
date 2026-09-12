package com.diasmart.springapi.inventory.repository;

import com.diasmart.springapi.inventory.entity.InventoryReading;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReadingRepository
        extends JpaRepository<InventoryReading, Long> {

    Page<InventoryReading> findByPatientId(
            Long patientId,
            Pageable pageable
    );

    InventoryReading
    findTopByPatientIdOrderByMeasuredAtDesc(
            Long patientId
    );
}