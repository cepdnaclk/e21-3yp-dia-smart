package com.diasmart.springapi.glucose.repository;

import com.diasmart.springapi.glucose.entity.GlucoseReading;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlucoseReadingRepository
        extends JpaRepository<GlucoseReading, Long> {

    Page<GlucoseReading> findByPatientId(
            Long patientId,
            Pageable pageable
    );

    GlucoseReading
    findTopByPatientIdOrderByMeasuredAtDesc(
            Long patientId
    );

    boolean existsByDeviceIdAndGlucometerSequenceNumber(
            Long deviceId,
            Integer glucometerSequenceNumber
    );
}
