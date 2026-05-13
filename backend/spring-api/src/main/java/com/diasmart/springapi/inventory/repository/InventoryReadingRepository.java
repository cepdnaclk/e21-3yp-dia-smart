package com.diasmart.springapi.inventory.repository;

import com.diasmart.springapi.inventory.entity.InventoryReading;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;


// Handles inventory database operations
public interface InventoryReadingRepository
        extends JpaRepository<InventoryReading, Long> {

    // Fetch latest inventory readings of patient
    Page<InventoryReading>
    findByPatientIdOrderByMeasuredAtDesc(

            Long patientId,

            Pageable pageable
    );
}