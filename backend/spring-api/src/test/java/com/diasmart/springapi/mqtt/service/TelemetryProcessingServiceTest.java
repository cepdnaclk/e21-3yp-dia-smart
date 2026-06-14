package com.diasmart.springapi.mqtt.service;

import com.diasmart.springapi.alerts.service.InventoryAlertEvaluationService;
import com.diasmart.springapi.alerts.service.StorageAlertEvaluationService;
import com.diasmart.springapi.audit.service.AuditService;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.repository.DeviceHealthLogRepository;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.dose.entity.DoseEvent;
import com.diasmart.springapi.dose.repository.DoseEventRepository;
import com.diasmart.springapi.glucose.entity.GlucoseReading;
import com.diasmart.springapi.glucose.repository.GlucoseReadingRepository;
import com.diasmart.springapi.inventory.entity.InventoryReading;
import com.diasmart.springapi.inventory.repository.InventoryReadingRepository;
import com.diasmart.springapi.mqtt.dto.TelemetryPayloadDTO;
import com.diasmart.springapi.raw_events.entity.RawDeviceEvent;
import com.diasmart.springapi.raw_events.repository.RawDeviceEventRepository;
import com.diasmart.springapi.storage.entity.StorageReading;
import com.diasmart.springapi.storage.repository.StorageReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelemetryProcessingService Tests")
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
    private TelemetryProcessingService telemetryProcessingService;

    private TelemetryPayloadDTO testPayload;
    private RawDeviceEvent rawEvent;
    private Device testDevice;

    @BeforeEach
    void setUp() {
        testPayload = new TelemetryPayloadDTO();
        testPayload.setEventId("event-001");
        testPayload.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC).toString());
        testPayload.setReplayedEvent(false);

        rawEvent = new RawDeviceEvent();
        rawEvent.setRawEventId(1L);
        rawEvent.setSourceEventId("event-001");
        rawEvent.setDeviceUid("DS-INNER-0001");
        rawEvent.setPatientId(1L);
        rawEvent.setProcessingStatus("RECEIVED");

        testDevice = new Device();
        testDevice.setDeviceId(1L);
        testDevice.setDeviceUid("DS-INNER-0001");
        testDevice.setActive(true);
    }

    // =====================================================
    // PAYLOAD VALIDATION TESTS
    // =====================================================

    @Test
    @DisplayName("Should throw exception when payload is null")
    void testProcessNullPayload() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                () -> telemetryProcessingService.process(null, "{}"),
                "Telemetry payload is required");
    }

    @Test
    @DisplayName("Should accept null mqtt topic")
    void testProcessNullMqttTopic() {
        // Arrange
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(rawEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> telemetryProcessingService.process(testPayload, "{}", null));
    }

    @Test
    @DisplayName("Should handle process method with 2 parameters (rawJson only)")
    void testProcessWithRawJsonOnly() {
        // Arrange
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(rawEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> telemetryProcessingService.process(testPayload, "{}"));
    }

    // =====================================================
    // DUPLICATE EVENT DETECTION TESTS
    // =====================================================

    @Test
    @DisplayName("Should skip processing duplicate events with same device and event ID")
    void testProcessDuplicateEvent() {
        // Arrange
        when(rawRepository.existsByDeviceUidAndSourceEventId("DS-INNER-0001", "event-001"))
                .thenReturn(true);

        // Act
        telemetryProcessingService.process(testPayload, "{}", "test/topic");

        // Assert
        verify(rawRepository, never()).save(any(RawDeviceEvent.class));
        verify(auditService, times(1)).logDuplicateMqttEvent(
                anyLong(), anyLong(), anyString(), anyString(), anyString(), anyBoolean()
        );
    }

    @Test
    @DisplayName("Should not skip non-duplicate events")
    void testProcessNonDuplicateEvent() {
        // Arrange
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(rawEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act
        telemetryProcessingService.process(testPayload, "{}", "test/topic");

        // Assert
        verify(rawRepository, times(1)).save(any(RawDeviceEvent.class));
    }

    // =====================================================
    // RAW EVENT PERSISTENCE TESTS
    // =====================================================

    @Test
    @DisplayName("Should save raw device event")
    void testSaveRawEvent() {
        // Arrange
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(rawEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act
        telemetryProcessingService.process(testPayload, "{}", "test/topic");

        // Assert
        ArgumentCaptor<RawDeviceEvent> eventCaptor = ArgumentCaptor.forClass(RawDeviceEvent.class);
        verify(rawRepository, atLeastOnce()).save(eventCaptor.capture());
    }

    // =====================================================
    // REPLAY EVENT LOGGING TESTS
    // =====================================================

    @Test
    @DisplayName("Should log replay event when replayed flag is true")
    void testLogReplayEvent() {
        // Arrange
        testPayload.setReplayedEvent(true);
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(rawEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act
        telemetryProcessingService.process(testPayload, "{}", "test/topic");

        // Assert
        verify(auditService, times(1)).logMqttReplayEvent(
                anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), any(OffsetDateTime.class)
        );
    }

    @Test
    @DisplayName("Should not log replay event when replayed flag is false or null")
    void testNoReplayEventLogging() {
        // Arrange
        testPayload.setReplayedEvent(false);
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(rawEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act
        telemetryProcessingService.process(testPayload, "{}", "test/topic");

        // Assert
        verify(auditService, never()).logMqttReplayEvent(
                anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), any(OffsetDateTime.class)
        );
    }

    // =====================================================
    // EVENT TYPE NORMALIZATION TESTS
    // =====================================================

    @Test
    @DisplayName("Should normalize event types to valid set")
    void testNormalizeEventType() {
        // Arrange
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(rawEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act
        telemetryProcessingService.process(testPayload, "{}", "test/topic");

        // Assert
        ArgumentCaptor<RawDeviceEvent> eventCaptor = ArgumentCaptor.forClass(RawDeviceEvent.class);
        verify(rawRepository, atLeastOnce()).save(eventCaptor.capture());
        String eventType = eventCaptor.getValue().getEventType();
        assertTrue(eventType != null && !eventType.isEmpty());
    }

    // =====================================================
    // PROCESSING STATUS TESTS
    // =====================================================

    @Test
    @DisplayName("Should set processing status to RECEIVED when raw event is first saved")
    void testRawEventProcessingStatus() {
        // Arrange
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(rawEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act
        telemetryProcessingService.process(testPayload, "{}", "test/topic");

        // Assert
        ArgumentCaptor<RawDeviceEvent> eventCaptor = ArgumentCaptor.forClass(RawDeviceEvent.class);
        verify(rawRepository, atLeastOnce()).save(eventCaptor.capture());
        assertEquals("RECEIVED", eventCaptor.getAllValues().get(0).getProcessingStatus());
    }

    // =====================================================
    // MQTT TOPIC HANDLING TESTS
    // =====================================================

    @Test
    @DisplayName("Should store mqtt topic when provided")
    void testMqttTopicStorage() {
        // Arrange
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(rawEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        String mqttTopic = "devices/inner-unit-001/telemetry";

        // Act
        telemetryProcessingService.process(testPayload, "{}", mqttTopic);

        // Assert
        ArgumentCaptor<RawDeviceEvent> eventCaptor = ArgumentCaptor.forClass(RawDeviceEvent.class);
        verify(rawRepository, atLeastOnce()).save(eventCaptor.capture());
    }

    // =====================================================
    // DEVICE RESOLUTION TESTS
    // =====================================================

    @Test
    @DisplayName("Should use existing device if found")
    void testReuseExistingDevice() {
        // Arrange
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(rawEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.of(testDevice));

        // Act
        telemetryProcessingService.process(testPayload, "{}", "test/topic");

        // Assert
        verify(deviceRepository, never()).save(any(Device.class));
    }

    // =====================================================
    // NULL EVENT ID TESTS
    // =====================================================

    @Test
    @DisplayName("Should handle null event ID")
    void testNullEventId() {
        // Arrange
        testPayload.setEventId(null);
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(rawEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act
        telemetryProcessingService.process(testPayload, "{}", "test/topic");

        // Assert
        verify(rawRepository, times(1)).save(any(RawDeviceEvent.class));
    }

    // =====================================================
    // TIMESTAMP PARSING TESTS
    // =====================================================

    @Test
    @DisplayName("Should parse valid ISO8601 timestamp")
    void testParseValidTimestamp() {
        // Arrange
        String validTimestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
        testPayload.setTimestamp(validTimestamp);
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(rawEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act & Assert
        assertDoesNotThrow(() -> telemetryProcessingService.process(testPayload, "{}", "test/topic"));
    }

    @Test
    @DisplayName("Should handle null timestamp by using current time")
    void testNullTimestamp() {
        // Arrange
        testPayload.setTimestamp(null);
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(rawEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act & Assert
        assertDoesNotThrow(() -> telemetryProcessingService.process(testPayload, "{}", "test/topic"));
    }

    // =====================================================
    // EDGE CASES
    // =====================================================

    @Test
    @DisplayName("Should handle empty raw JSON string")
    void testEmptyRawJson() {
        // Arrange
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(rawEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act & Assert
        assertDoesNotThrow(() -> telemetryProcessingService.process(testPayload, ""));
    }

    @Test
    @DisplayName("Should handle whitespace device UID")
    void testWhitespaceDeviceUid() {
        // Arrange
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(rawEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act & Assert
        assertDoesNotThrow(() -> telemetryProcessingService.process(testPayload, "{}", "test/topic"));
    }

    @Test
    @DisplayName("Should handle multiple consecutive payloads")
    void testMultipleConsecutivePayloads() {
        // Arrange
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(rawEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act
        for (int i = 0; i < 5; i++) {
            TelemetryPayloadDTO payload = new TelemetryPayloadDTO();
            payload.setEventId("event-" + i);
            payload.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC).toString());
            payload.setReplayedEvent(false);
            
            telemetryProcessingService.process(payload, "{}", "test/topic");
        }

        // Assert
        verify(rawRepository, atLeastOnce()).save(any(RawDeviceEvent.class));
    }

    @Test
    @DisplayName("Should handle special characters in event ID")
    void testSpecialCharactersInEventId() {
        // Arrange
        testPayload.setEventId("event-!@#$%^&*()_+-=[]{}|;:',.<>?");
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(rawEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act & Assert
        assertDoesNotThrow(() -> telemetryProcessingService.process(testPayload, "{}", "test/topic"));
    }

    @Test
    @DisplayName("Should handle very long raw JSON")
    void testLongRawJson() {
        // Arrange
        String longJson = "{" + "\"field\": \"" + "a".repeat(10000) + "\"}";
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(rawEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act & Assert
        assertDoesNotThrow(() -> telemetryProcessingService.process(testPayload, longJson, "test/topic"));
    }

    @Test
    @DisplayName("Should handle invalid JSON in raw data")
    void testInvalidJsonRawData() {
        // Arrange
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(rawEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act & Assert
        assertDoesNotThrow(() -> telemetryProcessingService.process(testPayload, "not valid json", "test/topic"));
    }

    @Test
    @DisplayName("Should preserve mqtt topic in raw event")
    void testMqttTopicPreservation() {
        // Arrange
        String mqttTopic = "devices/patient-1/inner-unit/telemetry";
        when(rawRepository.existsByDeviceUidAndSourceEventId(anyString(), anyString()))
                .thenReturn(false);
        
        RawDeviceEvent savedEvent = new RawDeviceEvent();
        savedEvent.setMqttTopic(mqttTopic);
        when(rawRepository.save(any(RawDeviceEvent.class))).thenReturn(savedEvent);
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(java.util.Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act
        telemetryProcessingService.process(testPayload, "{}", mqttTopic);

        // Assert
        ArgumentCaptor<RawDeviceEvent> eventCaptor = ArgumentCaptor.forClass(RawDeviceEvent.class);
        verify(rawRepository, atLeastOnce()).save(eventCaptor.capture());
    }
}
