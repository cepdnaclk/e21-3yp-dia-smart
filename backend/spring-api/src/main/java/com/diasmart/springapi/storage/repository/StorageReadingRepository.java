package com.diasmart.springapi.storage.repository;

import com.diasmart.springapi.storage.entity.StorageReading;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageReadingRepository
        extends JpaRepository<StorageReading, Long> {

    StorageReading
    findTopByPatientIdOrderByCreatedAtDesc(
            Long patientId
    );

    StorageReading
    findTopByPatientIdOrderByMeasuredAtDesc(
            Long patientId
    );
}
