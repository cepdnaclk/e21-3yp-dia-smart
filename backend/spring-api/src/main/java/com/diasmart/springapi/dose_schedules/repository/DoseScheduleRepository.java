package com.diasmart.springapi.dose_schedules.repository;

import com.diasmart.springapi.dose_schedules.entity.DoseSchedule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoseScheduleRepository
        extends JpaRepository<DoseSchedule, Long> {

    Page<DoseSchedule>
    findByPatientIdAndActiveTrue(
            Long patientId,
            Pageable pageable
    );

    List<DoseSchedule> findByPatientIdAndActiveTrue(Long patientId);

}