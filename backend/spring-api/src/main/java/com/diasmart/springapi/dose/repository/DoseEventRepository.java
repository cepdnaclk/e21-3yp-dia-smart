package com.diasmart.springapi.dose.repository;

import com.diasmart.springapi.dose.entity.DoseEvent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface DoseEventRepository
        extends JpaRepository<DoseEvent, Long> {

    Page<DoseEvent> findByPatientIdOrderByInjectedAtDesc(
        Long patientId,
        Pageable pageable
);

    DoseEvent
    findTopByPatientIdOrderByInjectedAtDesc(
            Long patientId
    );

    List<DoseEvent> findByPatientIdAndInjectedAtBetween(
        Long patientId,
        OffsetDateTime start,
        OffsetDateTime end
);
}