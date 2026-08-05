package com.diasmart.springapi.inventory.repository;

import com.diasmart.springapi.inventory.entity.InventoryReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface InventoryReadingRepository
        extends JpaRepository<InventoryReading, Long> {

    Page<InventoryReading> findByPatientId(
            Long patientId,
            Pageable pageable
    );

    InventoryReading findTopByPatientIdOrderByMeasuredAtDesc(
            Long patientId
    );

    InventoryReading findTopByPatientIdAndMeasuredAtLessThanEqualOrderByMeasuredAtDesc(
            Long patientId,
            OffsetDateTime to
    );

    @Query(
        "SELECT COUNT(i) FROM InventoryReading i WHERE i.patientId = :patientId AND i.measuredAt >= :from AND i.measuredAt <= :to AND (i.inventoryStatus = :lowStatus OR i.inventoryStatus = :critStatus)"
    )
    long countShortageEvents(
            @Param("patientId") Long patientId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("lowStatus") String lowStatus,
            @Param("critStatus") String critStatus
    );
}