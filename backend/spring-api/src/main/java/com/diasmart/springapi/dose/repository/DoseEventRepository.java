package com.diasmart.springapi.dose.repository;

import com.diasmart.springapi.dose.entity.DoseEvent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}