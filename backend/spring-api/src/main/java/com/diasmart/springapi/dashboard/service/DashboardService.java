package com.diasmart.springapi.dashboard.service;

import com.diasmart.springapi.alerts.dto.AlertResponse;
import com.diasmart.springapi.alerts.service.AlertService;
import com.diasmart.springapi.dashboard.dto.DashboardSummaryResponse;
import com.diasmart.springapi.dose.dto.DoseEventResponse;
import com.diasmart.springapi.glucose.dto.GlucoseReadingResponse;
import com.diasmart.springapi.inventory.dto.InventoryReadingResponse;
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;
import com.diasmart.springapi.storage.dto.StorageReadingResponse;

import org.springframework.stereotype.Service;

import java.util.List;

import com.diasmart.springapi.dashboard.service.bridge.GlucoseServiceBridge;
import com.diasmart.springapi.dashboard.service.bridge.DoseServiceBridge;
import com.diasmart.springapi.dashboard.service.bridge.StorageServiceBridge;
import com.diasmart.springapi.dashboard.service.bridge.InventoryServiceBridge;

@Service
public class DashboardService {

    private final GlucoseServiceBridge glucoseServiceBridge;

    private final DoseServiceBridge doseServiceBridge;

    private final StorageServiceBridge storageServiceBridge;

    private final InventoryServiceBridge inventoryServiceBridge;

    private final AlertService alertService;
    private final AuthorizationService authorizationService;

    public DashboardService(
            GlucoseServiceBridge glucoseServiceBridge,
            DoseServiceBridge doseServiceBridge,
            StorageServiceBridge storageServiceBridge,
            InventoryServiceBridge inventoryServiceBridge,
            AlertService alertService,
            AuthorizationService authorizationService
    ) {
        this.glucoseServiceBridge =
                glucoseServiceBridge;

        this.doseServiceBridge =
                doseServiceBridge;

        this.storageServiceBridge =
                storageServiceBridge;

        this.inventoryServiceBridge =
                inventoryServiceBridge;

        this.alertService = alertService;
        this.authorizationService = authorizationService;
    }

    public DashboardSummaryResponse
    getDashboardSummary(
            Long patientId
    ) {
        authorizationService.authorize(Permission.READ_DASHBOARD, patientId);

        DashboardSummaryResponse response =
                new DashboardSummaryResponse();

        GlucoseReadingResponse glucose =
                glucoseServiceBridge
                        .getLatestReading(patientId);

        DoseEventResponse dose =
                doseServiceBridge
                        .getLatestDose(patientId);

        StorageReadingResponse storage =
                storageServiceBridge
                        .getLatestStorage(patientId);

        InventoryReadingResponse inventory =
                inventoryServiceBridge
                        .getLatestInventory(patientId);

        List<AlertResponse> alerts =
                alertService
                        .getLatestAlertsForPatient(patientId, 5);

        response.setLatestGlucoseReading(
                glucose
        );

        response.setLatestDoseEvent(
                dose
        );

        response.setLatestStorageReading(
                storage
        );

        response.setLatestInventoryReading(
                inventory
        );

        response.setActiveAlerts(
                alerts
        );

        return response;
    }
}