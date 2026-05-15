package com.diasmart.springapi.alerts.controller;

import com.diasmart.springapi.alerts.dto.TestStorageAlertRequest;
import com.diasmart.springapi.alerts.service.StorageAlertEvaluationService;
import com.diasmart.springapi.shared.dto.ApiResponse;
import com.diasmart.springapi.storage.entity.StorageReading;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.diasmart.springapi.alerts.dto.TestInventoryAlertRequest;
import com.diasmart.springapi.alerts.service.InventoryAlertEvaluationService;
import com.diasmart.springapi.inventory.entity.InventoryReading;

@RestController
@RequestMapping("/api/v1/testing")
public class AlertTestingController {

    private final StorageAlertEvaluationService
            storageAlertEvaluationService;

    private final InventoryAlertEvaluationService
        inventoryAlertEvaluationService;

    public AlertTestingController(

        StorageAlertEvaluationService
                storageAlertEvaluationService,

        InventoryAlertEvaluationService
                inventoryAlertEvaluationService
) {

    this.storageAlertEvaluationService =
            storageAlertEvaluationService;

    this.inventoryAlertEvaluationService =
            inventoryAlertEvaluationService;
}

    /**
     * Development/testing endpoint only.
     *
     * Current phase:
     * manually triggers alert evaluation.
     *
     * Future:
     * alerts will be triggered automatically
     * from telemetry ingestion pipelines.
     */
    @PostMapping("/storage-alert-test")
    public ResponseEntity<ApiResponse<String>>
    testStorageAlert(

            @RequestBody
            TestStorageAlertRequest request
    ) {

        StorageReading reading =
                new StorageReading();

        reading.setPatientId(
                request.getPatientId()
        );

        reading.setTemperatureC(
                request.getTemperatureC()
        );

        storageAlertEvaluationService
                .evaluateStorageAlerts(reading);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Storage alert evaluation completed",
                        "TEST_COMPLETED"
                )
        );
    }

    /**
     * Development/testing endpoint only.
     *
     * Current phase:
     * manually triggers inventory alert evaluation.
     *
     * Future:
     * alerts will be triggered automatically
     * from telemetry ingestion pipelines.
     */
    @PostMapping("/inventory-alert-test")
    public ResponseEntity<ApiResponse<String>>
    testInventoryAlert(

            @RequestBody
            TestInventoryAlertRequest request
    ) {

        InventoryReading reading =
                new InventoryReading();

        reading.setPatientId(
                request.getPatientId()
        );

        reading.setEstimatedRemainingPercent(
                request.getEstimatedRemainingPercent()
        );

        inventoryAlertEvaluationService
                .evaluateInventoryAlerts(reading);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Inventory alert evaluation completed",
                        "TEST_COMPLETED"
                )
        );
    }
}