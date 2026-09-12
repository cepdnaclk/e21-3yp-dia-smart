package com.diasmart.springapi.storage.repository;

import com.diasmart.springapi.storage.entity.StorageReading;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageReadingRepository
        extends JpaRepository<StorageReading, Long> {

    Page<StorageReading> findByPatientId(
            Long patientId,
            Pageable pageable
    );

    StorageReading
    findTopByPatientIdOrderByCreatedAtDesc(
            Long patientId
    );

    StorageReading
    findTopByPatientIdOrderByMeasuredAtDesc(
            Long patientId
    );
}