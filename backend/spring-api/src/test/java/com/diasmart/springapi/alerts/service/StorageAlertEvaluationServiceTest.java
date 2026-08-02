package com.diasmart.springapi.alerts.service;

import com.diasmart.springapi.storage.entity.StorageReading;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageAlertEvaluationServiceTest {

    @Mock
    private AlertFactoryService alertFactoryService;

    @InjectMocks
    private StorageAlertEvaluationService service;

    @Test
    void shouldIgnoreNullReading() {

        service.evaluateStorageAlerts(null);

        verifyNoInteractions(alertFactoryService);
    }

    @Test
    void shouldIgnoreNullTemperature() {

        StorageReading reading = mock(StorageReading.class);

        when(reading.getTemperatureC())
                .thenReturn(null);

        service.evaluateStorageAlerts(reading);

        verifyNoInteractions(alertFactoryService);
    }

    @Test
    void shouldCreateLowTemperatureAlert() {

        StorageReading reading = mock(StorageReading.class);

        when(reading.getTemperatureC())
                .thenReturn(1.5);

        when(reading.getPatientId())
                .thenReturn(1L);

        service.evaluateStorageAlerts(reading);

        verify(alertFactoryService)
                .createAlert(
                        eq(1L),
                        eq("TEMP_LOW"),
                        eq("CRITICAL"),
                        eq("Storage temperature too low"),
                        contains("1.5")
                );
    }

    @Test
    void shouldCreateHighTemperatureAlert() {

        StorageReading reading = mock(StorageReading.class);

        when(reading.getTemperatureC())
                .thenReturn(10.0);

        when(reading.getPatientId())
                .thenReturn(1L);

        service.evaluateStorageAlerts(reading);

        verify(alertFactoryService)
                .createAlert(
                        eq(1L),
                        eq("TEMP_HIGH"),
                        eq("CRITICAL"),
                        eq("Storage temperature too high"),
                        contains("10.0")
                );
    }

    @Test
    void shouldNotCreateAlertForSafeTemperature() {

        StorageReading reading = mock(StorageReading.class);

        when(reading.getTemperatureC())
                .thenReturn(5.0);

        service.evaluateStorageAlerts(reading);

        verifyNoInteractions(alertFactoryService);
    }
}