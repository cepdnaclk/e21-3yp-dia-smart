package com.diasmart.springapi.alerts.service;

import com.diasmart.springapi.inventory.entity.InventoryReading;

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
 * - inventory telemetry ingestion APIs
 * - schedulers
 * - telemetry pipelines
 * - device event processors
 */
@Service
public class InventoryAlertEvaluationService {

    private final AlertFactoryService alertFactoryService;

    public InventoryAlertEvaluationService(
            AlertFactoryService alertFactoryService
    ) {
        this.alertFactoryService =
                alertFactoryService;
    }

    public void evaluateInventoryAlerts(
            InventoryReading reading
    ) {

        if (reading == null) {
            return;
        }

        Double remainingPercent =
                reading.getEstimatedRemainingPercent();

        if (remainingPercent == null) {
            return;
        }

        /*
         * Current implementation uses
         * fixed medically practical thresholds.
         *
         * Future versions may support:
         * - clinician customization
         * - patient-specific thresholds
         * - insulin-type adjustments
         */

        double warningThreshold = 20.0;

        double criticalThreshold = 10.0;

        // =========================
        // CRITICAL_INVENTORY
        // =========================

        if (remainingPercent <= criticalThreshold) {

            alertFactoryService.createAlert(

                    reading.getPatientId(),

                    "CRITICAL_INVENTORY",

                    "CRITICAL",

                    "Critical insulin inventory level",

                    "Remaining insulin inventory is critically low: "
                            + remainingPercent + "%"
            );

            return;
        }

        // =========================
        // LOW_INVENTORY
        // =========================

        if (remainingPercent <= warningThreshold) {

            alertFactoryService.createAlert(

                    reading.getPatientId(),

                    "LOW_INVENTORY",

                    "MEDIUM",

                    "Low insulin inventory",

                    "Remaining insulin inventory is low: "
                            + remainingPercent + "%"
            );
        }
    }
}