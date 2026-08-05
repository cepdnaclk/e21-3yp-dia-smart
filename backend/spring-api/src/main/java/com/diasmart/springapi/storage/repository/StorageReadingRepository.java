package com.diasmart.springapi.storage.repository;

import com.diasmart.springapi.storage.entity.StorageReading;
import com.diasmart.springapi.ai.dto.StorageStatsProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface StorageReadingRepository
        extends JpaRepository<StorageReading, Long> {

    Page<StorageReading> findByPatientId(
            Long patientId,
            Pageable pageable
    );

    StorageReading findTopByPatientIdOrderByCreatedAtDesc(
            Long patientId
    );

    StorageReading findTopByPatientIdOrderByMeasuredAtDesc(
            Long patientId
    );

    List<StorageReading> findByPatientIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
            Long patientId,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    );

    @Query(
        "SELECT COUNT(s) as count, AVG(s.temperatureC) as average, MIN(s.temperatureC) as minimum, MAX(s.temperatureC) as maximum " +
        "FROM StorageReading s WHERE s.patientId = :patientId AND s.measuredAt >= :from AND s.measuredAt <= :to"
    )
    StorageStatsProjection getStorageStats(
            @Param("patientId") Long patientId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Query(
        "SELECT COUNT(s) FROM StorageReading s WHERE s.patientId = :patientId AND s.measuredAt >= :from AND s.measuredAt <= :to AND (s.temperatureC < :minTemp OR s.temperatureC > :maxTemp)"
    )
    long countExcursions(
            @Param("patientId") Long patientId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("minTemp") Double minTemp,
            @Param("maxTemp") Double maxTemp
    );
}