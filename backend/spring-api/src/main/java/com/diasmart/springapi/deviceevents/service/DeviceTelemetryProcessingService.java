package com.diasmart.springapi.deviceevents.service;

import com.diasmart.springapi.careplan.service.DoseScheduleMatchingService;
import com.diasmart.springapi.careplan.service.DoseScheduleMatchingService.MatchResult;
import com.diasmart.springapi.deviceconfig.entity.DeviceCommand;
import com.diasmart.springapi.deviceconfig.entity.DeviceConfiguration;
import com.diasmart.springapi.deviceconfig.repository.DeviceCommandRepository;
import com.diasmart.springapi.deviceconfig.repository.DeviceConfigurationRepository;
import com.diasmart.springapi.deviceevents.entity.DeviceTelemetryEvent;
import com.diasmart.springapi.deviceevents.entity.ReminderEvent;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class DeviceTelemetryProcessingService {

    private static final int MQTT_QOS_ONE = 1;
    private static final String COMMAND_TYPE_WIFI_CONFIGURATION = "WIFI_CONFIGURATION";
    private static final String DEVICE_TYPE_OUTER = "OUTER_GATEWAY";
    private static final String DEVICE_TYPE_INNER = "INNER_UNIT";
    private static final int MAX_INNER_MESSAGE_LENGTH = 500;
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
    private static final Set<String> CONTROLLED_FAILURE_CODES = Set.of(
            "INNER_STAGE_FAILED",
            "INNER_CONNECTION_FAILED",
            "INNER_RESULT_TIMEOUT",
            "ROLLBACK_STARTED",
            "ROLLED_BACK",
            "RECOVERY_CHANNEL_ACTIVE"
    );
    private static final Set<String> CONTROLLED_ROLLBACK_STATUSES = Set.of(
            "NOT_REQUIRED",
            "ROLLBACK_STARTED",
            "ROLLED_BACK",
            "RECOVERY_CHANNEL_ACTIVE"
    );
    private static final Set<String> SENSITIVE_PAYLOAD_FIELDS = Set.of(
            "PASSWORD",
            "WIFI_PASSWORD",
            "WIFIPASSWORD",
            "WIFI_PASSWORD_CIPHERTEXT",
            "WIFIPASSWORDCIPHERTEXT",
            "WIFI_PASSWORD_NONCE",
            "WIFIPASSWORDNONCE",
            "WIFI_PASSWORD_TAG",
            "WIFIPASSWORDTAG",
            "CIPHERTEXT",
            "NONCE",
            "AUTH_TAG",
            "AUTHTAG",
            "JWT",
            "TOKEN",
            "CERTIFICATE",
            "PRIVATE_KEY",
            "PRIVATEKEY"
    );

    private final DeviceTelemetryEventRepository telemetryEventRepository;
    private final ReminderEventRepository reminderEventRepository;
    private final DoseEventRepository doseEventRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceCommandRepository commandRepository;
    private final DeviceConfigurationRepository configurationRepository;
    private final DeviceKitRepository kitRepository;
    private final DeviceKitDeviceRepository kitDeviceRepository;
    private final DoseScheduleMatchingService scheduleMatchingService;
    private final DeviceSyncService deviceSyncService;
    private final MqttService mqttService;
    private final ObjectMapper objectMapper;

    public DeviceTelemetryProcessingService(
            DeviceTelemetryEventRepository telemetryEventRepository,
            ReminderEventRepository reminderEventRepository,
            DoseEventRepository doseEventRepository,
            DeviceRepository deviceRepository,
            DeviceCommandRepository commandRepository,
            DeviceConfigurationRepository configurationRepository,
            DeviceKitRepository kitRepository,
            DeviceKitDeviceRepository kitDeviceRepository,
            DoseScheduleMatchingService scheduleMatchingService,
            DeviceSyncService deviceSyncService,
            MqttService mqttService,
            ObjectMapper objectMapper) {
        this.telemetryEventRepository = telemetryEventRepository;
        this.reminderEventRepository = reminderEventRepository;
        this.doseEventRepository = doseEventRepository;
        this.deviceRepository = deviceRepository;
        this.commandRepository = commandRepository;
        this.configurationRepository = configurationRepository;
        this.kitRepository = kitRepository;
        this.kitDeviceRepository = kitDeviceRepository;
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
        String topicOuterDeviceUid = resolveDeviceUidFromTopic(mqttTopic);
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
                safePayloadJson(payload),
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
                case "INNER_WIFI_CONFIGURATION_RESULT" -> processInnerWifiResult(payload, telemetryEvent, outerDevice, topicOuterDeviceUid);
                case "DEVICE_SYNC_REQUEST" -> deviceSyncService.recordAndResync(outerDevice, eventId, eventType);
                default -> throw new IllegalArgumentException("Unsupported device event type: " + eventType);
            }

            telemetryEvent.setProcessingStatus("PROCESSED");
            telemetryEvent.setAckStatus("ACCEPTED");
            telemetryEvent.setProcessingResult("ACCEPTED");
            telemetryEvent.setProcessedAt(OffsetDateTime.now(ZoneOffset.UTC));
            telemetryEventRepository.save(telemetryEvent);
            publishTelemetryAck(outerDeviceUid, eventId, "ACCEPTED");
        } catch (RuntimeException ex) {
            telemetryEvent.setProcessingStatus("FAILED");
            telemetryEvent.setAckStatus("REJECTED");
            telemetryEvent.setProcessingResult(safeProcessingResult(ex.getMessage()));
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

    private void processInnerWifiResult(
            JsonNode payload,
            DeviceTelemetryEvent telemetryEvent,
            Device outerDevice,
            String topicOuterDeviceUid
    ) {
        if (topicOuterDeviceUid == null) {
            throw reject("TOPIC_OUTER_UID_MISSING");
        }

        if (!topicOuterDeviceUid.equals(outerDevice.getDeviceUid())) {
            throw reject("REPORTING_OUTER_UID_MISMATCH");
        }

        String payloadOuterUid = firstNonBlank(text(payload, "outerDeviceId"), text(payload, "outerDeviceUid"));
        if (payloadOuterUid != null && !payloadOuterUid.equals(topicOuterDeviceUid)) {
            throw reject("PAYLOAD_OUTER_UID_MISMATCH");
        }

        String publicCommandId = text(payload, "commandId");
        if (publicCommandId == null || publicCommandId.isBlank()) {
            throw reject("COMMAND_ID_MISSING");
        }

        DeviceCommand command = findCommand(publicCommandId)
                .orElseThrow(() -> reject("COMMAND_NOT_FOUND"));

        if (!COMMAND_TYPE_WIFI_CONFIGURATION.equals(command.getCommandType())) {
            throw reject("COMMAND_TYPE_MISMATCH");
        }

        if (command.getDeviceId() == null || !command.getDeviceId().equals(outerDevice.getDeviceId())) {
            throw reject("REPORTING_OUTER_UID_MISMATCH");
        }

        if (command.getPatientId() == null || !command.getPatientId().equals(outerDevice.getPatientId())) {
            throw reject("DEVICE_PATIENT_MISMATCH");
        }

        if (command.getDeviceConfigurationId() == null || command.getConfigurationVersion() == null) {
            throw reject("COMMAND_CONFIGURATION_REFERENCE_MISSING");
        }

        DeviceConfiguration config = configurationRepository.findByConfigurationId(command.getDeviceConfigurationId())
                .orElseThrow(() -> reject("CONFIGURATION_NOT_FOUND"));

        if (!command.getDeviceId().equals(config.getOuterDeviceId())) {
            throw reject("CONFIGURATION_DEVICE_MISMATCH");
        }

        if (!command.getConfigurationVersion().equals(config.getConfigurationVersion())) {
            throw reject("COMMAND_SUPERSEDED");
        }

        Integer resultVersion = integer(payload, "configurationVersion");
        if (resultVersion == null) {
            throw reject("RESULT_CONFIGURATION_VERSION_MISSING");
        }

        if (!resultVersion.equals(command.getConfigurationVersion())) {
            throw reject("RESULT_CONFIGURATION_VERSION_MISMATCH");
        }

        String innerDeviceUid = firstNonBlank(text(payload, "innerDeviceId"), text(payload, "innerDeviceUid"));
        if (innerDeviceUid == null) {
            throw reject("INNER_DEVICE_UID_MISSING");
        }

        Device innerDevice = findDeviceByUidOrNumericId(innerDeviceUid)
                .orElseThrow(() -> reject("INNER_DEVICE_NOT_FOUND"));

        if (!DEVICE_TYPE_INNER.equals(innerDevice.getDeviceType())) {
            throw reject("INNER_DEVICE_TYPE_MISMATCH");
        }

        if (config.getInnerDeviceId() == null || !config.getInnerDeviceId().equals(innerDevice.getDeviceId())) {
            throw reject("CONFIGURATION_INNER_DEVICE_MISMATCH");
        }

        if (!config.getPatientId().equals(outerDevice.getPatientId())) {
            throw reject("DEVICE_PATIENT_MISMATCH");
        }

        if (!config.getPatientId().equals(innerDevice.getPatientId())) {
            throw reject("INNER_DEVICE_PATIENT_MISMATCH");
        }

        validateSameKit(outerDevice, innerDevice, config);

        InnerWifiResultStatus resultStatus = InnerWifiResultStatus.fromFirmware(text(payload, "status"));
        OffsetDateTime resultAt = parseTimestamp(text(payload, "timestamp"));

        telemetryEvent.setCommandId(command.getCommandId());
        telemetryEvent.setCommandUid(command.getCommandUid());
        telemetryEvent.setDeviceConfigurationId(config.getConfigurationId());
        telemetryEvent.setConfigurationVersion(resultVersion);
        telemetryEvent.setInnerDeviceId(innerDevice.getDeviceId());
        telemetryEvent.setInnerDeviceUid(innerDevice.getDeviceUid());

        applyInnerWifiResult(payload, command, config, resultStatus, resultAt);
    }

    private void applyInnerWifiResult(
            JsonNode payload,
            DeviceCommand command,
            DeviceConfiguration config,
            InnerWifiResultStatus resultStatus,
            OffsetDateTime resultAt
    ) {
        config.setInnerUnitStatus(resultStatus.name());
        config.setInnerUnitIpAddress(truncate(text(payload, "ipAddress"), 64));
        config.setInnerUnitMessage(truncate(text(payload, "message"), MAX_INNER_MESSAGE_LENGTH));
        config.setLastInnerUnitStatusAt(resultAt);

        if (resultStatus.successful()) {
            applySuccessfulInnerResult(command, config, resultAt);
        } else if (resultStatus == InnerWifiResultStatus.STAGED) {
            moveCommandIfOpen(command, "STAGED");
            config.setConfigurationStatus("STAGED");
        } else if (resultStatus == InnerWifiResultStatus.CONNECTING) {
            moveCommandIfOpen(command, "APPLYING");
            config.setConfigurationStatus("APPLYING");
        } else if (resultStatus == InnerWifiResultStatus.WAITING_FOR_CONFIGURATION) {
            moveCommandIfOpen(command, "STAGED");
            config.setConfigurationStatus("STAGED");
        } else {
            applyFailedInnerResult(payload, command, config, resultStatus, resultAt);
        }

        commandRepository.save(command);
        configurationRepository.save(config);
    }

    private void applySuccessfulInnerResult(DeviceCommand command, DeviceConfiguration config, OffsetDateTime resultAt) {
        config.setProvisioningFailureCode(null);
        config.setProvisioningFailureMessage(null);
        config.setRollbackStatus("NOT_REQUIRED");

        if ("APPLIED".equals(config.getOuterUnitStatus()) && "CONNECTED".equals(config.getMqttStatus())) {
            command.setCommandStatus("APPLIED");
            command.setCompletedAt(resultAt);
            command.setLastError(null);

            config.setConfigurationStatus("APPLIED");
            config.setProvisioningCompletedAt(resultAt);
            config.setLastSyncedAt(resultAt);
            config.setLastSuccessfulConfigurationId(config.getConfigurationId());
            config.setLastSuccessfulConfigurationVersion(config.getConfigurationVersion());
            config.setLastSuccessfulAt(resultAt);
        } else {
            moveCommandIfOpen(command, "APPLYING");
            config.setConfigurationStatus("APPLYING");
        }
    }

    private void applyFailedInnerResult(
            JsonNode payload,
            DeviceCommand command,
            DeviceConfiguration config,
            InnerWifiResultStatus resultStatus,
            OffsetDateTime resultAt
    ) {
        String failureCode = controlledFailureCode(payload, resultStatus);
        String rollbackStatus = controlledRollbackStatus(payload, resultStatus);

        command.setCommandStatus(resultStatus == InnerWifiResultStatus.ROLLED_BACK ? "ROLLED_BACK" : "FAILED");
        command.setLastError(failureCode);
        command.setCompletedAt(resultAt);
        command.setNextRetryAt(null);

        config.setConfigurationStatus(resultStatus == InnerWifiResultStatus.ROLLED_BACK ? "ROLLED_BACK" : "FAILED");
        config.setProvisioningFailureCode(failureCode);
        config.setProvisioningFailureMessage(truncate(text(payload, "message"), MAX_INNER_MESSAGE_LENGTH));
        config.setRollbackStatus(rollbackStatus);
        config.setProvisioningCompletedAt(resultAt);
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
        return firstNonBlank(
                resolveDeviceUidFromTopic(topic),
                firstNonBlank(text(payload, "outerDeviceId"), text(payload, "outerDeviceUid"))
        );
    }

    private String resolveDeviceUidFromTopic(String topic) {
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

    private Optional<DeviceCommand> findCommand(String commandId) {
        return commandRepository.findByCommandUid(commandId)
                .or(() -> parseNumericId(commandId).flatMap(commandRepository::findById));
    }

    private Optional<Device> findDeviceByUidOrNumericId(String deviceUidOrId) {
        return deviceRepository.findByDeviceUid(deviceUidOrId)
                .or(() -> parseNumericId(deviceUidOrId).flatMap(deviceRepository::findById));
    }

    private Optional<Long> parseNumericId(String value) {
        if (value == null) {
            return Optional.empty();
        }

        try {
            if (value.startsWith("CMD-")) {
                return Optional.of(Long.parseLong(value.substring(4)));
            }
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private void validateSameKit(Device outerDevice, Device innerDevice, DeviceConfiguration config) {
        if (!DEVICE_TYPE_OUTER.equals(outerDevice.getDeviceType())) {
            throw reject("OUTER_DEVICE_TYPE_MISMATCH");
        }

        DeviceKitDevice outerKitDevice = kitDeviceRepository.findByDeviceId(outerDevice.getDeviceId())
                .orElseThrow(() -> reject("OUTER_KIT_NOT_FOUND"));
        DeviceKitDevice innerKitDevice = kitDeviceRepository.findByDeviceId(innerDevice.getDeviceId())
                .orElseThrow(() -> reject("INNER_KIT_NOT_FOUND"));

        if (!DEVICE_TYPE_OUTER.equals(outerKitDevice.getKitDeviceRole())
                || !DEVICE_TYPE_INNER.equals(innerKitDevice.getKitDeviceRole())) {
            throw reject("KIT_DEVICE_ROLE_MISMATCH");
        }

        if (!outerKitDevice.getDeviceKitId().equals(innerKitDevice.getDeviceKitId())) {
            throw reject("KIT_MISMATCH");
        }

        DeviceKit kit = kitRepository.findById(outerKitDevice.getDeviceKitId())
                .orElseThrow(() -> reject("OUTER_KIT_NOT_FOUND"));

        if (kit.getPatientId() == null || !kit.getPatientId().equals(config.getPatientId())) {
            throw reject("KIT_PATIENT_MISMATCH");
        }
    }

    private void moveCommandIfOpen(DeviceCommand command, String nextStatus) {
        if (command.getCompletedAt() != null) {
            return;
        }

        String currentStatus = command.getCommandStatus();
        if ("EXPIRED".equals(currentStatus) || "TIMED_OUT".equals(currentStatus)) {
            return;
        }

        command.setCommandStatus(nextStatus);
    }

    private String controlledFailureCode(JsonNode payload, InnerWifiResultStatus status) {
        String supplied = normalize(firstNonBlank(text(payload, "failureCode"), text(payload, "errorCode")));
        if (supplied != null && CONTROLLED_FAILURE_CODES.contains(supplied)) {
            return supplied;
        }

        return switch (status) {
            case STAGED -> "INNER_STAGE_FAILED";
            case ROLLED_BACK -> "ROLLED_BACK";
            case RECOVERY_CHANNEL -> "RECOVERY_CHANNEL_ACTIVE";
            default -> "INNER_CONNECTION_FAILED";
        };
    }

    private String controlledRollbackStatus(JsonNode payload, InnerWifiResultStatus status) {
        String supplied = normalize(text(payload, "rollbackStatus"));
        if (supplied != null && CONTROLLED_ROLLBACK_STATUSES.contains(supplied)) {
            return supplied;
        }

        if (status == InnerWifiResultStatus.ROLLED_BACK) {
            return "ROLLED_BACK";
        }
        if (status == InnerWifiResultStatus.RECOVERY_CHANNEL) {
            return "RECOVERY_CHANNEL_ACTIVE";
        }
        return "NOT_REQUIRED";
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

    private String safePayloadJson(JsonNode payload) {
        if (payload == null) {
            return "{}";
        }

        JsonNode safePayload = payload.deepCopy();
        removeSensitiveFields(safePayload);
        try {
            return objectMapper.writeValueAsString(safePayload);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private void removeSensitiveFields(JsonNode node) {
        if (node == null) {
            return;
        }

        if (node instanceof ObjectNode objectNode) {
            ArrayList<String> fieldsToRemove = new ArrayList<>();
            Iterator<String> fieldNames = objectNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (isSensitiveField(fieldName)) {
                    fieldsToRemove.add(fieldName);
                }
            }
            fieldsToRemove.forEach(objectNode::remove);
            objectNode.fields().forEachRemaining(entry -> removeSensitiveFields(entry.getValue()));
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::removeSensitiveFields);
        }
    }

    private boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }

        String normalized = fieldName.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        return SENSITIVE_PAYLOAD_FIELDS.contains(normalized)
                || SENSITIVE_PAYLOAD_FIELDS.contains(normalized.replace("_", ""));
    }

    private IllegalArgumentException reject(String processingResult) {
        return new IllegalArgumentException(processingResult);
    }

    private String safeProcessingResult(String value) {
        String normalized = normalize(value);
        if (normalized == null || normalized.length() > 60) {
            return "FAILED";
        }

        for (int index = 0; index < normalized.length(); index++) {
            char character = normalized.charAt(index);
            if ((character < 'A' || character > 'Z')
                    && (character < '0' || character > '9')
                    && character != '_') {
                return "FAILED";
            }
        }

        return normalized;
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
