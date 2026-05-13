package com.diasmart.springapi.inventory.repository;

import com.diasmart.springapi.inventory.entity.InventoryReading;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReadingRepository
        extends JpaRepository<InventoryReading, Long> {

    InventoryReading
    findTopByPatientIdOrderByMeasuredAtDesc(
            Long patientId
    );
}
