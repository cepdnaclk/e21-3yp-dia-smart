package com.diasmart.springapi.deviceevents.service;

import com.diasmart.springapi.careplan.service.DoseScheduleMatchingService;
import com.diasmart.springapi.deviceconfig.entity.DeviceCommand;
import com.diasmart.springapi.deviceconfig.entity.DeviceConfiguration;
import com.diasmart.springapi.deviceconfig.repository.DeviceCommandRepository;
import com.diasmart.springapi.deviceconfig.repository.DeviceConfigurationRepository;
import com.diasmart.springapi.deviceevents.entity.DeviceTelemetryEvent;
import com.diasmart.springapi.deviceevents.repository.DeviceTelemetryEventRepository;
import com.diasmart.springapi.deviceevents.repository.ReminderEventRepository;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.entity.DeviceKit;
import com.diasmart.springapi.devices.entity.DeviceKitDevice;
import com.diasmart.springapi.devices.repository.DeviceKitDeviceRepository;
import com.diasmart.springapi.devices.repository.DeviceKitRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    private DeviceCommandRepository commandRepository;

    @Mock
    private DeviceConfigurationRepository configurationRepository;

    @Mock
    private DeviceKitRepository kitRepository;

    @Mock
    private DeviceKitDeviceRepository kitDeviceRepository;

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
                commandRepository,
                configurationRepository,
                kitRepository,
                kitDeviceRepository,
                scheduleMatchingService,
                deviceSyncService,
                mqttService,
                objectMapper
        );
    }

    @Test
    void processDoseRecordedShouldPersistDoseAndPublishAcceptedAck() throws Exception {
        Device outer = outerDevice();
        Device pen = new Device();
        pen.setDeviceId(3L);
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
        assertEquals(3L, doseCaptor.getValue().getDeviceId());
        assertEquals(100L, doseCaptor.getValue().getScheduleId());

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

    @Test
    void processInnerWifiConnectedShouldCorrelateCommandConfigInnerAndSanitizePayload() throws Exception {
        Device outer = outerDevice();
        Device inner = innerDevice(2L, "INNER-001");
        DeviceConfiguration config = configuration();
        config.setConfigurationVersion(3);
        config.setOuterUnitStatus("APPLIED");
        config.setMqttStatus("CONNECTED");
        config.setInnerUnitStatus("CONNECTING");
        DeviceCommand command = wifiCommand(25L, config);
        command.setCommandStatus("APPLIED");

        String rawJson = """
                {
                  "eventId": "INNER-WIFI-1001",
                  "commandId": "CMD-25",
                  "eventType": "INNER_WIFI_CONFIGURATION_RESULT",
                  "outerDeviceId": "OUTER-001",
                  "innerDeviceId": "INNER-001",
                  "configurationVersion": 3,
                  "status": "CONNECTED",
                  "ipAddress": "192.168.1.22",
                  "message": "Inner Unit connected",
                  "wifiPassword": "should-not-be-stored",
                  "timestamp": "2026-08-02T12:32:00Z"
                }
                """;

        stubLedger();
        when(deviceRepository.findByDeviceUid("OUTER-001")).thenReturn(Optional.of(outer));
        when(deviceRepository.findByDeviceUid("INNER-001")).thenReturn(Optional.of(inner));
        when(commandRepository.findByCommandUid("CMD-25")).thenReturn(Optional.of(command));
        when(configurationRepository.findByConfigurationId(11L)).thenReturn(Optional.of(config));
        when(kitDeviceRepository.findByDeviceId(1L)).thenReturn(Optional.of(kitDevice(1L, "OUTER_GATEWAY")));
        when(kitDeviceRepository.findByDeviceId(2L)).thenReturn(Optional.of(kitDevice(2L, "INNER_UNIT")));
        when(kitRepository.findById(99L)).thenReturn(Optional.of(activatedKit()));

        service.process(objectMapper.readTree(rawJson), rawJson, "diasmart/devices/OUTER-001/telemetry");

        assertEquals("CONNECTED", config.getInnerUnitStatus());
        assertEquals("APPLIED", config.getConfigurationStatus());
        assertEquals(3, config.getLastSuccessfulConfigurationVersion());
        assertNotNull(command.getCompletedAt());

        ArgumentCaptor<DeviceTelemetryEvent> eventCaptor = ArgumentCaptor.forClass(DeviceTelemetryEvent.class);
        verify(telemetryEventRepository, atLeast(2)).save(eventCaptor.capture());
        DeviceTelemetryEvent processedEvent = eventCaptor.getAllValues().get(eventCaptor.getAllValues().size() - 1);
        assertEquals(25L, processedEvent.getCommandId());
        assertEquals("CMD-25", processedEvent.getCommandUid());
        assertEquals(11L, processedEvent.getDeviceConfigurationId());
        assertEquals(3, processedEvent.getConfigurationVersion());
        assertEquals(2L, processedEvent.getInnerDeviceId());
        assertFalse(processedEvent.getPayload().contains("should-not-be-stored"));
        assertEquals("ACCEPTED", processedEvent.getProcessingResult());

        verify(configurationRepository).save(config);
        verify(commandRepository).save(command);
    }

    @Test
    void processInnerWifiFromWrongInnerShouldRejectWithoutUpdatingConfiguration() throws Exception {
        Device outer = outerDevice();
        Device wrongInner = innerDevice(3L, "INNER-OTHER");
        DeviceConfiguration config = configuration();
        DeviceCommand command = wifiCommand(25L, config);

        String rawJson = """
                {
                  "eventId": "INNER-WIFI-1002",
                  "commandId": "CMD-25",
                  "eventType": "INNER_WIFI_CONFIGURATION_RESULT",
                  "outerDeviceId": "OUTER-001",
                  "innerDeviceId": "INNER-OTHER",
                  "configurationVersion": 3,
                  "status": "CONNECTED",
                  "timestamp": "2026-08-02T12:32:00Z"
                }
                """;

        stubLedger();
        when(deviceRepository.findByDeviceUid("OUTER-001")).thenReturn(Optional.of(outer));
        when(deviceRepository.findByDeviceUid("INNER-OTHER")).thenReturn(Optional.of(wrongInner));
        when(commandRepository.findByCommandUid("CMD-25")).thenReturn(Optional.of(command));
        when(configurationRepository.findByConfigurationId(11L)).thenReturn(Optional.of(config));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.process(objectMapper.readTree(rawJson), rawJson, "diasmart/devices/OUTER-001/telemetry")
        );

        assertEquals("CONFIGURATION_INNER_DEVICE_MISMATCH", exception.getMessage());
        assertEquals("WAITING_FOR_CONFIGURATION", config.getInnerUnitStatus());
        verify(configurationRepository, never()).save(any(DeviceConfiguration.class));
        verify(commandRepository, never()).save(any(DeviceCommand.class));

        ArgumentCaptor<DeviceTelemetryEvent> eventCaptor = ArgumentCaptor.forClass(DeviceTelemetryEvent.class);
        verify(telemetryEventRepository, atLeast(2)).save(eventCaptor.capture());
        DeviceTelemetryEvent failedEvent = eventCaptor.getAllValues().get(eventCaptor.getAllValues().size() - 1);
        assertEquals("FAILED", failedEvent.getProcessingStatus());
        assertEquals("CONFIGURATION_INNER_DEVICE_MISMATCH", failedEvent.getProcessingResult());
    }

    private void stubLedger() {
        when(telemetryEventRepository.existsByEventId(any())).thenReturn(false);
        when(telemetryEventRepository.save(any(DeviceTelemetryEvent.class))).thenAnswer(invocation -> {
            DeviceTelemetryEvent event = invocation.getArgument(0);
            if (event.getTelemetryEventId() == null) {
                event.setTelemetryEventId(55L);
            }
            return event;
        });
    }

    private Device outerDevice() {
        Device device = new Device();
        device.setDeviceId(1L);
        device.setPatientId(10L);
        device.setDeviceUid("OUTER-001");
        device.setDeviceType("OUTER_GATEWAY");
        device.setActive(true);
        return device;
    }

    private Device innerDevice(Long deviceId, String uid) {
        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setPatientId(10L);
        device.setDeviceUid(uid);
        device.setDeviceType("INNER_UNIT");
        device.setActive(true);
        return device;
    }

    private DeviceConfiguration configuration() {
        DeviceConfiguration config = new DeviceConfiguration();
        config.setConfigurationId(11L);
        config.setOuterDeviceId(1L);
        config.setPatientId(10L);
        config.setInnerDeviceId(2L);
        config.setConfigurationVersion(3);
        config.setConfigurationStatus("STAGED");
        config.setInnerUnitStatus("WAITING_FOR_CONFIGURATION");
        config.setMqttStatus("PENDING");
        config.setRollbackStatus("NOT_REQUIRED");
        return config;
    }

    private DeviceCommand wifiCommand(Long commandId, DeviceConfiguration config) {
        DeviceCommand command = new DeviceCommand();
        command.setCommandId(commandId);
        command.setCommandUid("CMD-" + commandId);
        command.setDeviceId(config.getOuterDeviceId());
        command.setPatientId(config.getPatientId());
        command.setDeviceConfigurationId(config.getConfigurationId());
        command.setConfigurationVersion(config.getConfigurationVersion());
        command.setCommandType("WIFI_CONFIGURATION");
        command.setCommandStatus("PUBLISHED");
        command.setPayload("{}");
        return command;
    }

    private DeviceKitDevice kitDevice(Long deviceId, String role) {
        DeviceKitDevice kitDevice = new DeviceKitDevice();
        kitDevice.setDeviceKitId(99L);
        kitDevice.setDeviceId(deviceId);
        kitDevice.setKitDeviceRole(role);
        return kitDevice;
    }

    private DeviceKit activatedKit() {
        DeviceKit kit = new DeviceKit();
        kit.setDeviceKitId(99L);
        kit.setPatientId(10L);
        kit.setStatus("ACTIVATED");
        return kit;
    }
}
