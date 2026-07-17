package com.diasmart.springapi.deviceevents.service;

import com.diasmart.springapi.careplan.service.DoseScheduleMatchingService;
import com.diasmart.springapi.careplan.service.DoseScheduleMatchingService.MatchResult;
import com.diasmart.springapi.deviceconfig.entity.DeviceConfiguration;
import com.diasmart.springapi.deviceconfig.repository.DeviceConfigurationRepository;
import com.diasmart.springapi.deviceevents.entity.DeviceTelemetryEvent;
import com.diasmart.springapi.deviceevents.entity.ReminderEvent;
import com.diasmart.springapi.deviceevents.repository.DeviceTelemetryEventRepository;
import com.diasmart.springapi.deviceevents.repository.ReminderEventRepository;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.dose.entity.DoseEvent;
import com.diasmart.springapi.dose.repository.DoseEventRepository;
import com.diasmart.springapi.mqtt.service.MqttService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class DeviceTelemetryProcessingService {

    private static final int MQTT_QOS_ONE = 1;
    private static final Set<String> NEW_EVENT_TYPES = Set.of(
            "DOSE_RECORDED",
            "REMINDER_STARTED",
            "REMINDER_REPEATED",
            "REMINDER_MANUALLY_STOPPED",
            "DOSE_MISSED",
            "POSSIBLE_DOUBLE_DOSE",
            "INNER_WIFI_CONFIGURATION_RESULT",
            "DEVICE_SYNC_REQUEST"
    );

    private final DeviceTelemetryEventRepository telemetryEventRepository;
    private final ReminderEventRepository reminderEventRepository;
    private final DoseEventRepository doseEventRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceConfigurationRepository configurationRepository;
    private final DoseScheduleMatchingService scheduleMatchingService;
    private final DeviceSyncService deviceSyncService;
    private final MqttService mqttService;
    private final ObjectMapper objectMapper;

    public DeviceTelemetryProcessingService(
            DeviceTelemetryEventRepository telemetryEventRepository,
            ReminderEventRepository reminderEventRepository,
            DoseEventRepository doseEventRepository,
            DeviceRepository deviceRepository,
            DeviceConfigurationRepository configurationRepository,
            DoseScheduleMatchingService scheduleMatchingService,
            DeviceSyncService deviceSyncService,
            MqttService mqttService,
            ObjectMapper objectMapper) {
        this.telemetryEventRepository = telemetryEventRepository;
        this.reminderEventRepository = reminderEventRepository;
        this.doseEventRepository = doseEventRepository;
        this.deviceRepository = deviceRepository;
        this.configurationRepository = configurationRepository;
        this.scheduleMatchingService = scheduleMatchingService;
        this.deviceSyncService = deviceSyncService;
        this.mqttService = mqttService;
        this.objectMapper = objectMapper;
    }

    public boolean supports(JsonNode payload) {
        String eventType = normalize(text(payload, "eventType"));
        return eventType != null && NEW_EVENT_TYPES.contains(eventType);
    }

    @Transactional
    public void process(JsonNode payload, String rawJson, String mqttTopic) {
        String eventId = text(payload, "eventId");
        String eventType = normalize(text(payload, "eventType"));
        String outerDeviceUid = resolveOuterDeviceUid(payload, mqttTopic);

        if (outerDeviceUid == null) {
            throw new IllegalArgumentException("outerDeviceId is required for device telemetry");
        }

        if (eventId == null || eventId.isBlank()) {
            publishTelemetryAck(outerDeviceUid, null, "REJECTED");
            throw new IllegalArgumentException("eventId is required for device telemetry");
        }

        if (telemetryEventRepository.existsByEventId(eventId)) {
            publishTelemetryAck(outerDeviceUid, eventId, "DUPLICATE");
            return;
        }

        Device outerDevice = deviceRepository.findByDeviceUid(outerDeviceUid)
                .orElseThrow(() -> new IllegalArgumentException("Registered outer device not found: " + outerDeviceUid));

        DeviceTelemetryEvent telemetryEvent = saveLedgerEvent(
                payload,
                rawJson,
                mqttTopic,
                eventId,
                eventType,
                outerDevice
        );

        try {
            switch (eventType) {
                case "DOSE_RECORDED" -> processDose(payload, telemetryEvent, outerDevice);
                case "REMINDER_STARTED", "REMINDER_REPEATED", "REMINDER_MANUALLY_STOPPED",
                        "DOSE_MISSED", "POSSIBLE_DOUBLE_DOSE" -> processReminder(payload, telemetryEvent, outerDevice);
                case "INNER_WIFI_CONFIGURATION_RESULT" -> processInnerWifiResult(payload, outerDevice);
                case "DEVICE_SYNC_REQUEST" -> deviceSyncService.recordAndResync(outerDevice, eventId, eventType);
                default -> throw new IllegalArgumentException("Unsupported device event type: " + eventType);
            }

            telemetryEvent.setProcessingStatus("PROCESSED");
            telemetryEvent.setAckStatus("ACCEPTED");
            telemetryEvent.setProcessedAt(OffsetDateTime.now(ZoneOffset.UTC));
            telemetryEventRepository.save(telemetryEvent);
            publishTelemetryAck(outerDeviceUid, eventId, "ACCEPTED");
        } catch (RuntimeException ex) {
            telemetryEvent.setProcessingStatus("FAILED");
            telemetryEvent.setAckStatus("REJECTED");
            telemetryEvent.setProcessingError(truncate(ex.getMessage(), 1000));
            telemetryEvent.setProcessedAt(OffsetDateTime.now(ZoneOffset.UTC));
            telemetryEventRepository.save(telemetryEvent);
            publishTelemetryAck(outerDeviceUid, eventId, "REJECTED");
            throw ex;
        }
    }

    private DeviceTelemetryEvent saveLedgerEvent(
            JsonNode payload,
            String rawJson,
            String mqttTopic,
            String eventId,
            String eventType,
            Device outerDevice
    ) {
        OffsetDateTime timestamp = parseTimestamp(firstNonBlank(
                text(payload, "timestamp"),
                text(payload, "takenAt"),
                text(payload, "secondDoseTime"),
                text(payload, "firstDoseTime")
        ));

        DeviceTelemetryEvent event = new DeviceTelemetryEvent();
        event.setEventId(eventId);
        event.setEventType(eventType);
        event.setOuterDeviceId(outerDevice.getDeviceId());
        event.setOuterDeviceUid(outerDevice.getDeviceUid());
        event.setPatientId(outerDevice.getPatientId());
        event.setCarePlanVersion(integer(payload, "carePlanVersion"));
        event.setScheduleExternalId(text(payload, "scheduleId"));
        event.setEventTimestamp(timestamp);
        event.setMqttTopic(mqttTopic);
        event.setPayload(rawJson);
        event.setProcessingStatus("RECEIVED");
        event.setAckStatus("REJECTED");
        event.setReceivedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return telemetryEventRepository.save(event);
    }

    private void processDose(JsonNode payload, DeviceTelemetryEvent telemetryEvent, Device outerDevice) {
        BigDecimal doseUnits = decimal(payload, "doseUnits");
        if (doseUnits == null) {
            throw new IllegalArgumentException("doseUnits is required for dose telemetry");
        }

        OffsetDateTime takenAt = parseTimestamp(firstNonBlank(text(payload, "takenAt"), text(payload, "timestamp")));
        Optional<MatchResult> match = scheduleMatchingService.match(
                outerDevice.getPatientId(),
                integer(payload, "carePlanVersion"),
                text(payload, "scheduleId"),
                takenAt,
                doseUnits
        );

        DoseEvent doseEvent = new DoseEvent();
        doseEvent.setPatientId(outerDevice.getPatientId());
        doseEvent.setDeviceId(resolvePenDeviceId(payload));
        doseEvent.setInjectedAt(takenAt);
        doseEvent.setDoseUnits(doseUnits.doubleValue());
        doseEvent.setDetectionMethod("BLE_NOTIFY");
        doseEvent.setEventStatus("CONFIRMED");
        doseEvent.setDoseStatus(firstNonBlank(text(payload, "status"), "UNMATCHED"));
        doseEvent.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        doseEvent.setNotes("sourceEventId=" + telemetryEvent.getEventId());

        match.ifPresent(result -> {
            doseEvent.setScheduleId(result.getScheduleId());
            doseEvent.setPrescriptionId(result.getPrescriptionId());
            telemetryEvent.setScheduleExternalId(result.getScheduleExternalId());
        });

        doseEventRepository.save(doseEvent);
    }

    private void processReminder(JsonNode payload, DeviceTelemetryEvent telemetryEvent, Device outerDevice) {
        OffsetDateTime timestamp = parseTimestamp(text(payload, "timestamp"));
        Optional<MatchResult> match = scheduleMatchingService.match(
                outerDevice.getPatientId(),
                integer(payload, "carePlanVersion"),
                text(payload, "scheduleId"),
                timestamp,
                null
        );

        ReminderEvent reminderEvent = new ReminderEvent();
        reminderEvent.setTelemetryEventId(telemetryEvent.getTelemetryEventId());
        reminderEvent.setPatientId(outerDevice.getPatientId());
        reminderEvent.setOuterDeviceId(outerDevice.getDeviceId());
        reminderEvent.setScheduleExternalId(text(payload, "scheduleId"));
        reminderEvent.setEventType(normalize(text(payload, "eventType")));
        reminderEvent.setCarePlanVersion(integer(payload, "carePlanVersion"));
        reminderEvent.setRepeatNumber(integer(payload, "repeatNumber"));
        reminderEvent.setWindowStart(localTime(payload, "windowStart"));
        reminderEvent.setTargetTime(localTime(payload, "targetTime"));
        reminderEvent.setWindowEnd(localTime(payload, "windowEnd"));
        reminderEvent.setEventTimestamp(timestamp);

        match.ifPresent(result -> {
            reminderEvent.setSourceScheduleId(result.getScheduleId());
            telemetryEvent.setScheduleExternalId(result.getScheduleExternalId());
        });

        reminderEventRepository.save(reminderEvent);
    }

    private void processInnerWifiResult(JsonNode payload, Device outerDevice) {
        DeviceConfiguration config = configurationRepository.findByOuterDeviceId(outerDevice.getDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("Device configuration not found for outer device"));

        config.setInnerUnitStatus(normalize(firstNonBlank(text(payload, "status"), "FAILED")));
        config.setInnerUnitIpAddress(text(payload, "ipAddress"));
        config.setInnerUnitMessage(text(payload, "message"));
        config.setLastInnerUnitStatusAt(parseTimestamp(text(payload, "timestamp")));
        configurationRepository.save(config);
    }

    private Long resolvePenDeviceId(JsonNode payload) {
        String penDeviceUid = text(payload, "penDeviceId");
        if (penDeviceUid == null) {
            return null;
        }

        return deviceRepository.findByDeviceUid(penDeviceUid)
                .map(Device::getDeviceId)
                .orElse(null);
    }

    private String resolveOuterDeviceUid(JsonNode payload, String topic) {
        String fromPayload = firstNonBlank(text(payload, "outerDeviceId"), text(payload, "outerDeviceUid"));
        if (fromPayload != null) {
            return fromPayload;
        }

        if (topic == null) {
            return null;
        }

        String[] parts = topic.split("/");
        for (int index = 0; index < parts.length - 1; index++) {
            if ("devices".equals(parts[index])) {
                return parts[index + 1];
            }
        }

        return null;
    }

    private void publishTelemetryAck(String outerDeviceUid, String eventId, String status) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", eventId);
            payload.put("status", status);
            payload.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());

            mqttService.publish(
                    "diasmart/devices/" + outerDeviceUid + "/telemetry-ack",
                    objectMapper.writeValueAsString(payload),
                    MQTT_QOS_ONE,
                    false
            );
        } catch (JsonProcessingException ex) {
            System.out.println("Telemetry ACK serialization failed");
        } catch (RuntimeException ex) {
            System.out.println("Telemetry ACK publish failed: " + ex.getMessage());
        }
    }

    private String text(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Integer integer(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        return value == null || !value.canConvertToInt() ? null : value.asInt();
    }

    private BigDecimal decimal(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || !value.isNumber()) {
            return null;
        }

        return value.decimalValue();
    }

    private LocalTime localTime(JsonNode payload, String field) {
        String value = text(payload, field);
        return value == null ? null : LocalTime.parse(value);
    }

    private OffsetDateTime parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }

        return OffsetDateTime.parse(value);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        return second;
    }

    private String firstNonBlank(String first, String second, String third, String fourth) {
        String value = firstNonBlank(first, second);
        if (value != null) {
            return value;
        }
        value = firstNonBlank(third, fourth);
        return value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}
