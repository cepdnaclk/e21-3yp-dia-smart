package com.diasmart.springapi.glucose.repository;

import com.diasmart.springapi.glucose.entity.GlucoseReading;
import com.diasmart.springapi.ai.dto.GlucoseStatsProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface GlucoseReadingRepository
        extends JpaRepository<GlucoseReading, Long> {

    Page<GlucoseReading> findByPatientId(
            Long patientId,
            Pageable pageable
    );

    GlucoseReading findTopByPatientIdOrderByMeasuredAtDesc(
            Long patientId
    );

    boolean existsByDeviceIdAndGlucometerSequenceNumber(
            Long deviceId,
            Integer glucometerSequenceNumber
    );

    List<GlucoseReading> findByPatientIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
            Long patientId,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    );

    @Query(
        "SELECT COUNT(g) as count, AVG(g.glucoseValueMgDl) as average, MIN(g.glucoseValueMgDl) as minimum, MAX(g.glucoseValueMgDl) as maximum " +
        "FROM GlucoseReading g WHERE g.patientId = :patientId AND g.measuredAt >= :from AND g.measuredAt <= :to"
    )
    GlucoseStatsProjection getGlucoseStats(
            @Param("patientId") Long patientId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Query(
        "SELECT COUNT(g) FROM GlucoseReading g WHERE g.patientId = :patientId AND g.measuredAt >= :from AND g.measuredAt <= :to AND g.glucoseValueMgDl > :maxThreshold"
    )
    long countHighReadings(
            @Param("patientId") Long patientId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("maxThreshold") Double maxThreshold
    );

    @Query(
        "SELECT COUNT(g) FROM GlucoseReading g WHERE g.patientId = :patientId AND g.measuredAt >= :from AND g.measuredAt <= :to AND g.glucoseValueMgDl < :minThreshold"
    )
    long countLowReadings(
            @Param("patientId") Long patientId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("minThreshold") Double minThreshold
    );
}
