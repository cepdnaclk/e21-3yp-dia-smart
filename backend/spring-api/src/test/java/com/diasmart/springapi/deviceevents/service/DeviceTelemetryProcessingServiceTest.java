package com.diasmart.springapi.deviceevents.service;

import com.diasmart.springapi.careplan.service.DoseScheduleMatchingService;
import com.diasmart.springapi.deviceconfig.repository.DeviceConfigurationRepository;
import com.diasmart.springapi.deviceevents.entity.DeviceTelemetryEvent;
import com.diasmart.springapi.deviceevents.repository.DeviceTelemetryEventRepository;
import com.diasmart.springapi.deviceevents.repository.ReminderEventRepository;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.dose.entity.DoseEvent;
import com.diasmart.springapi.dose.repository.DoseEventRepository;
import com.diasmart.springapi.mqtt.service.MqttService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceTelemetryProcessingServiceTest {

    @Mock
    private DeviceTelemetryEventRepository telemetryEventRepository;

    @Mock
    private ReminderEventRepository reminderEventRepository;

    @Mock
    private DoseEventRepository doseEventRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceConfigurationRepository configurationRepository;

    @Mock
    private DoseScheduleMatchingService scheduleMatchingService;

    @Mock
    private DeviceSyncService deviceSyncService;

    @Mock
    private MqttService mqttService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DeviceTelemetryProcessingService service;

    @BeforeEach
    void setUp() {
        service = new DeviceTelemetryProcessingService(
                telemetryEventRepository,
                reminderEventRepository,
                doseEventRepository,
                deviceRepository,
                configurationRepository,
                scheduleMatchingService,
                deviceSyncService,
                mqttService,
                objectMapper
        );
    }

    @Test
    void processDoseRecordedShouldPersistDoseAndPublishAcceptedAck() throws Exception {
        Device outer = new Device();
        outer.setDeviceId(1L);
        outer.setPatientId(10L);
        outer.setDeviceUid("OUTER-001");

        Device pen = new Device();
        pen.setDeviceId(2L);
        pen.setDeviceUid("PEN-001");

        String rawJson = """
                {
                  "eventId": "DOSE-1",
                  "eventType": "DOSE_RECORDED",
                  "outerDeviceId": "OUTER-001",
                  "penDeviceId": "PEN-001",
                  "scheduleId": "SCH-100",
                  "carePlanVersion": 3,
                  "doseUnits": 10,
                  "takenAt": "2026-07-18T08:15:00+05:30",
                  "status": "TAKEN_WITHIN_WINDOW"
                }
                """;

        when(telemetryEventRepository.existsByEventId("DOSE-1")).thenReturn(false);
        when(deviceRepository.findByDeviceUid("OUTER-001")).thenReturn(Optional.of(outer));
        when(deviceRepository.findByDeviceUid("PEN-001")).thenReturn(Optional.of(pen));
        when(telemetryEventRepository.save(any(DeviceTelemetryEvent.class))).thenAnswer(invocation -> {
            DeviceTelemetryEvent event = invocation.getArgument(0);
            if (event.getTelemetryEventId() == null) {
                event.setTelemetryEventId(55L);
            }
            return event;
        });
        when(scheduleMatchingService.match(eq(10L), eq(3), eq("SCH-100"), any(OffsetDateTime.class), eq(BigDecimal.TEN)))
                .thenReturn(Optional.of(new DoseScheduleMatchingService.MatchResult(100L, 200L, "SCH-100")));

        service.process(objectMapper.readTree(rawJson), rawJson, "diasmart/devices/OUTER-001/telemetry");

        ArgumentCaptor<DoseEvent> doseCaptor = ArgumentCaptor.forClass(DoseEvent.class);
        verify(doseEventRepository).save(doseCaptor.capture());
        assertEquals(10.0, doseCaptor.getValue().getDoseUnits());
        assertEquals(2L, doseCaptor.getValue().getDeviceId());
        assertEquals(100L, doseCaptor.getValue().getScheduleId());
        assertEquals("TAKEN_WITHIN_WINDOW", doseCaptor.getValue().getDoseStatus());

        ArgumentCaptor<String> ackCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService).publish(eq("diasmart/devices/OUTER-001/telemetry-ack"), ackCaptor.capture(), eq(1), eq(false));
        assertEquals("ACCEPTED", objectMapper.readTree(ackCaptor.getValue()).get("status").asText());
    }

    @Test
    void processDuplicateEventShouldPublishDuplicateAckWithoutPersisting() throws Exception {
        String rawJson = """
                {
                  "eventId": "DOSE-1",
                  "eventType": "DOSE_RECORDED",
                  "outerDeviceId": "OUTER-001"
                }
                """;

        when(telemetryEventRepository.existsByEventId("DOSE-1")).thenReturn(true);

        service.process(objectMapper.readTree(rawJson), rawJson, "diasmart/devices/OUTER-001/telemetry");

        verify(telemetryEventRepository, never()).save(any());
        verify(doseEventRepository, never()).save(any());

        ArgumentCaptor<String> ackCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService).publish(eq("diasmart/devices/OUTER-001/telemetry-ack"), ackCaptor.capture(), eq(1), eq(false));
        assertEquals("DUPLICATE", objectMapper.readTree(ackCaptor.getValue()).get("status").asText());
    }
}
