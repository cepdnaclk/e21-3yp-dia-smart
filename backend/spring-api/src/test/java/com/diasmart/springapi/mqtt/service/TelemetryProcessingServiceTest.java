package com.diasmart.springapi.mqtt.service;

import com.diasmart.springapi.alerts.service.InventoryAlertEvaluationService;
import com.diasmart.springapi.alerts.service.StorageAlertEvaluationService;
import com.diasmart.springapi.audit.service.AuditService;
import com.diasmart.springapi.devices.repository.DeviceHealthLogRepository;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.dose.repository.DoseEventRepository;
import com.diasmart.springapi.glucose.repository.GlucoseReadingRepository;
import com.diasmart.springapi.inventory.repository.InventoryReadingRepository;
import com.diasmart.springapi.mqtt.dto.GatewayDTO;
import com.diasmart.springapi.mqtt.dto.TelemetryPayloadDTO;
import com.diasmart.springapi.raw_events.entity.RawDeviceEvent;
import com.diasmart.springapi.raw_events.repository.RawDeviceEventRepository;
import com.diasmart.springapi.storage.repository.StorageReadingRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryProcessingServiceTest {

    @Mock
    private GlucoseReadingRepository glucoseRepository;

    @Mock
    private StorageReadingRepository storageRepository;

    @Mock
    private RawDeviceEventRepository rawRepository;

    @Mock
    private InventoryReadingRepository inventoryRepository;

    @Mock
    private DoseEventRepository doseEventRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceHealthLogRepository healthLogRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private StorageAlertEvaluationService storageAlertEvaluationService;

    @Mock
    private InventoryAlertEvaluationService inventoryAlertEvaluationService;

    @InjectMocks
    private TelemetryProcessingService service;

    @Test
    void shouldRejectNullPayload() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.process(null, "{}")
        );
    }

    @Test
    void shouldSkipDuplicateEvent() {

        TelemetryPayloadDTO payload =
                new TelemetryPayloadDTO();

        payload.setEventId("EVT-001");

        GatewayDTO gateway =
                new GatewayDTO();

        gateway.setDeviceUid("GW-001");

        payload.setGateway(gateway);

        when(
                rawRepository
                        .existsByDeviceUidAndSourceEventId(
                                "GW-001",
                                "EVT-001"
                        )
        ).thenReturn(true);

        service.process(payload, "{}");

        verify(auditService)
                .logDuplicateMqttEvent(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );

        verify(rawRepository, never())
                .save(any());
    }

    @Test
    void shouldProcessEmptyPayload() {

        RawDeviceEvent rawEvent =
                new RawDeviceEvent();

        rawEvent.setRawEventId(1L);

        when(rawRepository.save(any()))
                .thenReturn(rawEvent);

        service.process(
                new TelemetryPayloadDTO(),
                "{}"
        );

        verify(rawRepository, atLeastOnce())
                .save(any());
    }

    @Test
    void shouldSaveRawEvent() {

        RawDeviceEvent rawEvent =
                new RawDeviceEvent();

        rawEvent.setRawEventId(1L);

        when(rawRepository.save(any()))
                .thenReturn(rawEvent);

        TelemetryPayloadDTO payload =
                new TelemetryPayloadDTO();

        service.process(payload, "{}");

        verify(rawRepository, atLeastOnce())
                .save(any());
    }
}
