package com.diasmart.springapi.alerts.service;

import com.diasmart.springapi.alerts.repository.AlertRepository;
import com.diasmart.springapi.storage.entity.StorageReading;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Current phase:
 * ----------------
 * Alert evaluation is independent/manual.
 *
 * Alerts are NOT automatically triggered yet.
 *
 * Future integration:
 * -------------------
 * Later this service will be called from:
 * - storage telemetry ingestion APIs
 * - schedulers
 * - telemetry pipelines
 * - device event processors
 */
@Service
public class StorageAlertEvaluationService {

    private static final Duration TEMPERATURE_ALERT_GAP =
            Duration.ofMinutes(10);

    private final AlertFactoryService alertFactoryService;
    private final AlertRepository alertRepository;

    public StorageAlertEvaluationService(
            AlertFactoryService alertFactoryService,
            AlertRepository alertRepository
    ) {
        this.alertFactoryService =
                alertFactoryService;
        this.alertRepository =
                alertRepository;
    }

    public void evaluateStorageAlerts(
            StorageReading reading
    ) {

        if (reading == null) {
            return;
        }

        Double temperature =
                reading.getTemperatureC();

        if (temperature == null) {
            return;
        }

        /*
        * Insulin safe refrigeration range:
        * 2°C - 8°C
        *
        * Current implementation uses
        * medically accepted fixed thresholds.
        *
        * Future versions may support:
        * - patient-specific settings
        * - insulin-type customization
        * - clinician-defined thresholds
        */

        double minSafeTemperature = 2.0;

        double maxSafeTemperature = 8.0;

        // =========================
        // TEMP_LOW
        // =========================

        if (temperature < minSafeTemperature) {

            createTemperatureAlert(

                    reading.getPatientId(),

                    "TEMP_LOW",

                    "CRITICAL",

                    "Storage temperature too low",

                    "Detected storage temperature below safe range: "
                            + temperature + "°C"
            );
        }

        // =========================
        // TEMP_HIGH
        // =========================

        if (temperature > maxSafeTemperature) {

            createTemperatureAlert(

                    reading.getPatientId(),

                    "TEMP_HIGH",

                    "CRITICAL",

                    "Storage temperature too high",

                    "Detected storage temperature above safe range: "
                            + temperature + "°C"
            );
        }
    }

    private void createTemperatureAlert(

            Long patientId,

            String alertType,

            String severity,

            String title,

            String message
    ) {

        OffsetDateTime gapStart =
                OffsetDateTime.now()
                        .minus(TEMPERATURE_ALERT_GAP);

        if (alertRepository
                .existsByPatientIdAndAlertTypeAndCreatedAtAfter(
                        patientId,
                        alertType,
                        gapStart
                )) {
            return;
        }

        alertFactoryService.createAlert(
                patientId,
                alertType,
                severity,
                title,
                message
        );
    }
}
