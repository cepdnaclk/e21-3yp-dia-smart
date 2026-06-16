package com.diasmart.springapi.dashboard.service;

import com.diasmart.springapi.alerts.dto.AlertResponse;
import com.diasmart.springapi.alerts.service.AlertService;
import com.diasmart.springapi.dashboard.dto.DashboardSummaryResponse;
import com.diasmart.springapi.dashboard.service.bridge.DoseServiceBridge;
import com.diasmart.springapi.dashboard.service.bridge.GlucoseServiceBridge;
import com.diasmart.springapi.dashboard.service.bridge.InventoryServiceBridge;
import com.diasmart.springapi.dashboard.service.bridge.StorageServiceBridge;
import com.diasmart.springapi.dose.dto.DoseEventResponse;
import com.diasmart.springapi.glucose.dto.GlucoseReadingResponse;
import com.diasmart.springapi.inventory.dto.InventoryReadingResponse;
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;
import com.diasmart.springapi.storage.dto.StorageReadingResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private GlucoseServiceBridge glucoseServiceBridge;

    @Mock
    private DoseServiceBridge doseServiceBridge;

    @Mock
    private StorageServiceBridge storageServiceBridge;

    @Mock
    private InventoryServiceBridge inventoryServiceBridge;

    @Mock
    private AlertService alertService;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void shouldReturnDashboardSummary() {

        Long patientId = 1L;

        GlucoseReadingResponse glucose = new GlucoseReadingResponse();
        DoseEventResponse dose = new DoseEventResponse();
        StorageReadingResponse storage = new StorageReadingResponse();
        InventoryReadingResponse inventory = new InventoryReadingResponse();

        List<AlertResponse> alerts = List.of(new AlertResponse());

        doNothing().when(authorizationService)
                .authorize(Permission.READ_DASHBOARD, patientId);

        when(glucoseServiceBridge.getLatestReading(patientId))
                .thenReturn(glucose);

        when(doseServiceBridge.getLatestDose(patientId))
                .thenReturn(dose);

        when(storageServiceBridge.getLatestStorage(patientId))
                .thenReturn(storage);

        when(inventoryServiceBridge.getLatestInventory(patientId))
                .thenReturn(inventory);

        when(alertService.getLatestAlertsForPatient(patientId, 5))
                .thenReturn(alerts);

        DashboardSummaryResponse result =
                dashboardService.getDashboardSummary(patientId);

        assertEquals(glucose, result.getLatestGlucoseReading());
        assertEquals(dose, result.getLatestDoseEvent());
        assertEquals(storage, result.getLatestStorageReading());
        assertEquals(inventory, result.getLatestInventoryReading());
        assertEquals(alerts, result.getActiveAlerts());
    }

    @Test
    void shouldCallAuthorization() {

        Long patientId = 1L;

        dashboardService.getDashboardSummary(patientId);

        verify(authorizationService)
                .authorize(Permission.READ_DASHBOARD, patientId);
    }

    @Test
    void shouldHandleNullData() {

        Long patientId = 1L;

        DashboardSummaryResponse result =
                dashboardService.getDashboardSummary(patientId);

        assertNotNull(result);
        assertNull(result.getLatestGlucoseReading());
        assertNull(result.getLatestDoseEvent());
    }

    @Test
    void shouldPropagateAuthorizationFailure() {

        Long patientId = 1L;

        doThrow(new RuntimeException("Forbidden"))
                .when(authorizationService)
                .authorize(Permission.READ_DASHBOARD, patientId);

        assertThrows(
                RuntimeException.class,
                () -> dashboardService.getDashboardSummary(patientId)
        );
    }
}