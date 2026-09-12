package com.diasmart.springapi.alerts.service;

import com.diasmart.springapi.alerts.entity.Alert;
import com.diasmart.springapi.alerts.repository.AlertRepository;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class AlertFactoryService {

    private final AlertRepository alertRepository;

    public AlertFactoryService(
            AlertRepository alertRepository
    ) {
        this.alertRepository = alertRepository;
    }

    /**
     * Current phase:
     * ----------------
     * This service creates alerts independently.
     *
     * Alerts are NOT automatically triggered yet.
     *
     * Future integration:
     * -------------------
     * Later this service will be called from:
     * - telemetry ingestion APIs
     * - scheduled evaluators
     * - event pipelines
     * - device processing services
     */
    public Alert createAlert(

            Long patientId,

            String alertType,

            String severity,

            String title,

            String message
    ) {

        Alert alert = new Alert();

        alert.setPatientId(patientId);

        alert.setAlertType(alertType);

        alert.setSeverity(severity);

        alert.setTitle(title);

        alert.setMessage(message);

        alert.setStatus("OPEN");

        alert.setFirstDetectedAt(
                OffsetDateTime.now()
        );

        alert.setLastDetectedAt(
                OffsetDateTime.now()
        );

        alert.setCreatedAt(
                OffsetDateTime.now()
        );

        alert.setUpdatedAt(
                OffsetDateTime.now()
        );

        return alertRepository.save(alert);
    }

    public Optional<Alert> createAlertIfGapElapsed(

            Long patientId,

            String alertType,

            String severity,

            String title,

            String message,

            Duration alertGap
    ) {

        Optional<Alert> latestAlert =
                alertRepository
                        .findTopByPatientIdAndAlertTypeOrderByCreatedAtDesc(
                                patientId,
                                alertType
                        );

        OffsetDateTime gapStart =
                OffsetDateTime.now()
                        .minus(alertGap);

        if (latestAlert.isPresent()
                && latestAlert.get().getCreatedAt() != null
                && latestAlert.get().getCreatedAt().isAfter(gapStart)) {

            return Optional.empty();
        }

        return Optional.of(
                createAlert(
                        patientId,
                        alertType,
                        severity,
                        title,
                        message
                )
        );
    }
}
