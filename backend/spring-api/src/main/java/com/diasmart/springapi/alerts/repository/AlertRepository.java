package com.diasmart.springapi.alerts.repository;

import com.diasmart.springapi.alerts.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface AlertRepository
                extends JpaRepository<Alert, Long> {

        Page<Alert> findAllByOrderByCreatedAtDesc(
                        Pageable pageable);

        Page<Alert> findByStatusOrderByCreatedAtDesc(
                        String status,
                        Pageable pageable);

        Page<Alert> findByPatientIdOrderByCreatedAtDesc(
                        Long patientId,
                        Pageable pageable);

        Page<Alert> findByPatientIdInOrderByCreatedAtDesc(
                        List<Long> patientIds,
                        Pageable pageable);

        Page<Alert> findByPatientIdInAndStatusOrderByCreatedAtDesc(
                        List<Long> patientIds,
                        String status,
                        Pageable pageable);

        List<Alert> findByPatientIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                        Long patientId,
                        OffsetDateTime from,
                        OffsetDateTime to,
                        Pageable pageable);
}
