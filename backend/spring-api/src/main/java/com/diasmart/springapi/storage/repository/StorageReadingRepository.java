package com.diasmart.springapi.storage.repository;

import com.diasmart.springapi.storage.entity.StorageReading;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;


// Handles storage database operations
public interface StorageReadingRepository
        extends JpaRepository<StorageReading, Long> {

    // Fetch patient storage history ordered latest first
    Page<StorageReading>
    findByPatientIdOrderByMeasuredAtDesc(

            Long patientId,

            Pageable pageable
    );
}