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
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.repository.DeviceHealthLogRepository;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.dose.entity.DoseEvent;
import com.diasmart.springapi.dose.repository.DoseEventRepository;
import com.diasmart.springapi.glucose.entity.GlucoseReading;
import com.diasmart.springapi.glucose.repository.GlucoseReadingRepository;
import com.diasmart.springapi.inventory.repository.InventoryReadingRepository;
import com.diasmart.springapi.mqtt.dto.DoseDTO;
import com.diasmart.springapi.mqtt.dto.GatewayDTO;
import com.diasmart.springapi.mqtt.dto.GlucoseDTO;
import com.diasmart.springapi.mqtt.dto.PatientDTO;
import com.diasmart.springapi.mqtt.dto.TelemetryPayloadDTO;
import com.diasmart.springapi.raw_events.entity.RawDeviceEvent;
import com.diasmart.springapi.raw_events.repository.RawDeviceEventRepository;
import com.diasmart.springapi.storage.repository.StorageReadingRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                                () -> service.process(null, "{}"));
        }

        @Test
        void shouldSkipDuplicateEvent() {

                TelemetryPayloadDTO payload = new TelemetryPayloadDTO();

                payload.setEventId("EVT-001");

                GatewayDTO gateway = new GatewayDTO();

                gateway.setDeviceUid("GW-001");

                payload.setGateway(gateway);

                when(
                                rawRepository
                                                .existsByDeviceUidAndSourceEventId(
                                                                "GW-001",
                                                                "EVT-001"))
                                .thenReturn(true);

                service.process(payload, "{}");

                verify(auditService)
                                .logDuplicateMqttEvent(
                                                any(),
                                                any(),
                                                any(),
                                                any(),
                                                any(),
                                                any());

                verify(rawRepository, never())
                                .save(any());
        }

        @Test
        void shouldProcessEmptyPayload() {

                RawDeviceEvent rawEvent = new RawDeviceEvent();

                rawEvent.setRawEventId(1L);

                when(rawRepository.save(any()))
                                .thenReturn(rawEvent);

                service.process(
                                new TelemetryPayloadDTO(),
                                "{}");

                verify(rawRepository, atLeastOnce())
                                .save(any());
        }

        @Test
        void shouldSaveRawEvent() {

                RawDeviceEvent rawEvent = new RawDeviceEvent();

                rawEvent.setRawEventId(1L);

                when(rawRepository.save(any()))
                                .thenReturn(rawEvent);

                TelemetryPayloadDTO payload = new TelemetryPayloadDTO();

                service.process(payload, "{}");

                verify(rawRepository, atLeastOnce())
                                .save(any());
        }

        @Test
        void shouldSkipDuplicateGlucoseSequenceAndSaveDose() {
                TelemetryPayloadDTO payload = combinedGlucoseAndDosePayload();

                when(rawRepository.existsByDeviceUidAndSourceEventId(
                                "OUTER-1",
                                "evt-1")).thenReturn(false);
                when(deviceRepository.findByDeviceUid(anyString()))
                                .thenReturn(Optional.empty());
                when(glucoseRepository
                                .existsByDeviceIdAndGlucometerSequenceNumber(20L, 77))
                                .thenReturn(true);

                doAnswer(invocation -> {
                        RawDeviceEvent event = invocation.getArgument(0);

                        if (event.getRawEventId() == null) {
                                event.setRawEventId(100L);
                        }

                        return event;
                }).when(rawRepository).save(any(RawDeviceEvent.class));

                doAnswer(invocation -> {
                        Device device = invocation.getArgument(0);

                        if (device.getDeviceId() == null) {
                                device.setDeviceId(deviceIdFor(device.getDeviceUid()));
                        }

                        return device;
                }).when(deviceRepository).save(any(Device.class));

                service.process(
                                payload,
                                """
                                                {
                                                  "eventId": "evt-1",
                                                  "eventType": "COMBINED_TELEMETRY"
                                                }
                                                """,
                                "diasmart/test");

                verify(glucoseRepository, never())
                                .save(any(GlucoseReading.class));

                ArgumentCaptor<DoseEvent> doseCaptor = ArgumentCaptor.forClass(DoseEvent.class);
                verify(doseEventRepository).save(doseCaptor.capture());
                assertEquals(4.5, doseCaptor.getValue().getDoseUnits());
                assertEquals(30L, doseCaptor.getValue().getDeviceId());

                ArgumentCaptor<RawDeviceEvent> rawCaptor = ArgumentCaptor.forClass(RawDeviceEvent.class);
                verify(rawRepository, org.mockito.Mockito.times(2))
                                .save(rawCaptor.capture());

                RawDeviceEvent processedEvent = rawCaptor.getAllValues().get(1);
                assertEquals("PROCESSED", processedEvent.getProcessingStatus());
                assertNull(processedEvent.getProcessingError());
        }

        @Test
        void shouldContinueOtherSectionsWhenGlucoseSaveFails() {
                TelemetryPayloadDTO payload = combinedGlucoseAndDosePayload();

                when(rawRepository.existsByDeviceUidAndSourceEventId(
                                "OUTER-1",
                                "evt-1")).thenReturn(false);
                when(deviceRepository.findByDeviceUid(anyString()))
                                .thenReturn(Optional.empty());

                doAnswer(invocation -> {
                        RawDeviceEvent event = invocation.getArgument(0);

                        if (event.getRawEventId() == null) {
                                event.setRawEventId(101L);
                        }

                        return event;
                }).when(rawRepository).save(any(RawDeviceEvent.class));

                doAnswer(invocation -> {
                        Device device = invocation.getArgument(0);

                        if (device.getDeviceId() == null) {
                                device.setDeviceId(deviceIdFor(device.getDeviceUid()));
                        }

                        return device;
                }).when(deviceRepository).save(any(Device.class));

                doThrow(new RuntimeException("duplicate glucose sequence"))
                                .when(glucoseRepository)
                                .save(any(GlucoseReading.class));

                service.process(
                                payload,
                                """
                                                {
                                                  "eventId": "evt-1",
                                                  "eventType": "COMBINED_TELEMETRY"
                                                }
                                                """,
                                "diasmart/test");

                verify(doseEventRepository).save(any(DoseEvent.class));

                ArgumentCaptor<RawDeviceEvent> rawCaptor = ArgumentCaptor.forClass(RawDeviceEvent.class);
                verify(rawRepository, org.mockito.Mockito.times(2))
                                .save(rawCaptor.capture());

                RawDeviceEvent processedEvent = rawCaptor.getAllValues().get(1);
                assertEquals("PROCESSED", processedEvent.getProcessingStatus());
                assertTrue(
                                processedEvent
                                                .getProcessingError()
                                                .contains("glucose: duplicate glucose sequence"));
        }

        private TelemetryPayloadDTO combinedGlucoseAndDosePayload() {
                TelemetryPayloadDTO payload = new TelemetryPayloadDTO();
                payload.setEventId("evt-1");
                payload.setEventType("COMBINED_TELEMETRY");
                payload.setTimestamp("2026-06-16T08:00:00Z");

                PatientDTO patient = new PatientDTO();
                patient.setPatientId(1L);
                payload.setPatient(patient);

                GatewayDTO gateway = new GatewayDTO();
                gateway.setDeviceUid("OUTER-1");
                payload.setGateway(gateway);

                GlucoseDTO glucose = new GlucoseDTO();
                glucose.setDeviceUid("GLU-1");
                glucose.setValueMgDl(145);
                glucose.setSequenceNumber(77);
                payload.setGlucose(glucose);

                DoseDTO dose = new DoseDTO();
                dose.setDeviceUid("PEN-1");
                dose.setDoseUnits(4.5);
                payload.setDose(dose);

                return payload;
        }

        private Long deviceIdFor(String deviceUid) {
                return switch (deviceUid) {
                        case "OUTER-1" -> 10L;
                        case "GLU-1" -> 20L;
                        case "PEN-1" -> 30L;
                        default -> 99L;
                };
        }
}
