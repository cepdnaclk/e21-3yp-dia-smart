package com.diasmart.springapi.alerts.service;

import com.diasmart.springapi.storage.entity.StorageReading;

import org.springframework.stereotype.Service;

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

    private final AlertFactoryService alertFactoryService;

    public StorageAlertEvaluationService(
            AlertFactoryService alertFactoryService
    ) {
        this.alertFactoryService =
                alertFactoryService;
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

            alertFactoryService.createAlert(

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

            alertFactoryService.createAlert(

                    reading.getPatientId(),

                    "TEMP_HIGH",

                    "CRITICAL",

                    "Storage temperature too high",

                    "Detected storage temperature above safe range: "
                            + temperature + "°C"
            );
        }
    }
}