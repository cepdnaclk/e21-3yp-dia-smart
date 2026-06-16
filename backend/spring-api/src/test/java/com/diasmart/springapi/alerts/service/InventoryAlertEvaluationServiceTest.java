package com.diasmart.springapi.alerts.service;

import com.diasmart.springapi.inventory.entity.InventoryReading;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryAlertEvaluationServiceTest {

    @Mock
    private AlertFactoryService alertFactoryService;

    @InjectMocks
    private InventoryAlertEvaluationService service;

    @Test
    void shouldIgnoreNullReading() {

        service.evaluateInventoryAlerts(null);

        verifyNoInteractions(alertFactoryService);
    }

    @Test
    void shouldIgnoreNullRemainingPercent() {

        InventoryReading reading =
                mock(InventoryReading.class);

        when(reading.getEstimatedRemainingPercent())
                .thenReturn(null);

        service.evaluateInventoryAlerts(reading);

        verifyNoInteractions(alertFactoryService);
    }

    @Test
    void shouldCreateCriticalInventoryAlert() {

        InventoryReading reading =
                mock(InventoryReading.class);

        when(reading.getEstimatedRemainingPercent())
                .thenReturn(5.0);

        when(reading.getPatientId())
                .thenReturn(1L);

        service.evaluateInventoryAlerts(reading);

        verify(alertFactoryService)
                .createAlert(
                        eq(1L),
                        eq("CRITICAL_INVENTORY"),
                        eq("CRITICAL"),
                        eq("Critical insulin inventory level"),
                        contains("5.0")
                );
    }

    @Test
    void shouldCreateLowInventoryAlert() {

        InventoryReading reading =
                mock(InventoryReading.class);

        when(reading.getEstimatedRemainingPercent())
                .thenReturn(15.0);

        when(reading.getPatientId())
                .thenReturn(1L);

        service.evaluateInventoryAlerts(reading);

        verify(alertFactoryService)
                .createAlert(
                        eq(1L),
                        eq("LOW_INVENTORY"),
                        eq("MEDIUM"),
                        eq("Low insulin inventory"),
                        contains("15.0")
                );
    }

    @Test
    void shouldNotCreateAlertForNormalInventory() {

        InventoryReading reading =
                mock(InventoryReading.class);

        when(reading.getEstimatedRemainingPercent())
                .thenReturn(50.0);

        service.evaluateInventoryAlerts(reading);

        verifyNoInteractions(alertFactoryService);
    }
}