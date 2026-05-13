package com.diasmart.springapi.storage.repository;

import com.diasmart.springapi.storage.entity.StorageReading;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;


// Handles storage database operations
public interface StorageReadingRepository
        extends JpaRepository<StorageReading, Long> {

    // Fetch paginated storage history
    Page<StorageReading>
    findByPatientIdOrderByMeasuredAtDesc(

            Long patientId,

            Pageable pageable
    );


    // Fetch latest inserted storage reading
    StorageReading
    findTopByPatientIdOrderByCreatedAtDesc(

            Long patientId
    );


    // Fetch latest measured storage reading
    StorageReading
    findTopByPatientIdOrderByMeasuredAtDesc(

            Long patientId
    );
}