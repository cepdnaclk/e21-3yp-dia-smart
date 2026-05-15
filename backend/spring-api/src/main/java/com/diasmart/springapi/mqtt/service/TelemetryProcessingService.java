package com.diasmart.springapi.mqtt.service;

import com.diasmart.springapi.audit.service.AuditService;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.entity.DeviceHealthLog;
import com.diasmart.springapi.devices.repository.DeviceHealthLogRepository;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.dose.entity.DoseEvent;
import com.diasmart.springapi.dose.repository.DoseEventRepository;
import com.diasmart.springapi.glucose.entity.GlucoseReading;
import com.diasmart.springapi.glucose.repository.GlucoseReadingRepository;
import com.diasmart.springapi.inventory.entity.InventoryReading;
import com.diasmart.springapi.inventory.repository.InventoryReadingRepository;
import com.diasmart.springapi.mqtt.dto.BatteryTelemetryDTO;
import com.diasmart.springapi.mqtt.dto.DoseDTO;
import com.diasmart.springapi.mqtt.dto.GlucoseDTO;
import com.diasmart.springapi.mqtt.dto.InventoryTelemetryDTO;
import com.diasmart.springapi.mqtt.dto.StorageTelemetryDTO;
import com.diasmart.springapi.mqtt.dto.TelemetryPayloadDTO;
import com.diasmart.springapi.raw_events.entity.RawDeviceEvent;
import com.diasmart.springapi.raw_events.repository.RawDeviceEventRepository;
import com.diasmart.springapi.storage.entity.StorageReading;
import com.diasmart.springapi.storage.repository.StorageReadingRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

//To integrate with alert evaluation services
import com.diasmart.springapi.alerts.service.StorageAlertEvaluationService;
import com.diasmart.springapi.alerts.service.InventoryAlertEvaluationService;

@Service
public class TelemetryProcessingService {

    private static final String DEFAULT_INNER_UNIT_UID =
            "DS-INNER-0001";

    private static final String DEFAULT_DOSE_CAP_UID =
            "DS-CAP-0001";

    private static final String DEFAULT_GLUCOMETER_UID =
            "DS-GLU-0001";

    private static final Set<String> EVENT_TYPES = Set.of(
            "STORAGE_READING",
            "INVENTORY_READING",
            "GLUCOSE_READING",
            "DOSE_EVENT",
            "DEVICE_HEALTH",
            "COMBINED_TELEMETRY",
            "UNKNOWN"
    );

    private static final Set<String> DEVICE_TYPES = Set.of(
            "INNER_UNIT",
            "OUTER_GATEWAY",
            "DOSE_CAP",
            "GLUCOMETER",
            "OTHER"
    );

    private static final Set<String> COMMUNICATION_TYPES = Set.of(
            "BLE",
            "ESP_NOW",
            "MQTTS",
            "HTTPS",
            "MANUAL",
            "OTHER"
    );

    private static final Set<String> DOOR_STATES = Set.of(
            "OPEN",
            "CLOSED",
            "UNKNOWN"
    );

    private static final Set<String> TEMPERATURE_STATUSES = Set.of(
            "SAFE",
            "LOW",
            "HIGH",
            "UNKNOWN"
    );

    private static final Set<String> INVENTORY_STATUSES = Set.of(
            "OK",
            "LOW",
            "CRITICAL",
            "EMPTY",
            "REMOVED",
            "UNKNOWN"
    );

    private static final Set<String> GLUCOSE_SOURCES = Set.of(
            "BLE_GLUCOMETER",
            "MANUAL",
            "ESTIMATED",
            "TEST"
    );

    private static final Set<String> MEAL_CONTEXTS = Set.of(
            "FASTING",
            "BEFORE_MEAL",
            "AFTER_MEAL",
            "BEDTIME",
            "RANDOM",
            "UNKNOWN"
    );

    private static final Set<String> DETECTION_METHODS = Set.of(
            "AS5600",
            "MANUAL",
            "BLE_NOTIFY",
            "ESTIMATED",
            "TEST"
    );

    private static final Set<String> DOSE_STATUSES = Set.of(
            "CONFIRMED",
            "PENDING",
            "REJECTED"
    );

    private static final Set<String> POWER_SOURCES = Set.of(
            "BATTERY",
            "ADAPTER",
            "USB",
            "UNKNOWN"
    );

    private static final Set<String> HEALTH_STATUSES = Set.of(
            "ONLINE",
            "OFFLINE",
            "LOW_BATTERY",
            "ERROR",
            "UNKNOWN"
    );

    /*
    * Alert evaluation services.
    *
    * Current phase:
    * alerts are generated after
    * successful telemetry persistence.
    *
    * Future:
    * may evolve into async/event-driven
    * alert processing pipeline.
    */
    private final StorageAlertEvaluationService
        storageAlertEvaluationService;

    private final InventoryAlertEvaluationService
        inventoryAlertEvaluationService;

    private final GlucoseReadingRepository glucoseRepository;
    private final StorageReadingRepository storageRepository;
    private final RawDeviceEventRepository rawRepository;
    private final InventoryReadingRepository inventoryRepository;
    private final DoseEventRepository doseEventRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceHealthLogRepository healthLogRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public TelemetryProcessingService(
            GlucoseReadingRepository glucoseRepository,
            StorageReadingRepository storageRepository,
            RawDeviceEventRepository rawRepository,
            InventoryReadingRepository inventoryRepository,
            DoseEventRepository doseEventRepository,
            DeviceRepository deviceRepository,
            DeviceHealthLogRepository healthLogRepository,
            AuditService auditService,

        //for alert integration    
        StorageAlertEvaluationService
                storageAlertEvaluationService,

        InventoryAlertEvaluationService
                inventoryAlertEvaluationService
    ) {
        this.glucoseRepository = glucoseRepository;
        this.storageRepository = storageRepository;
        this.rawRepository = rawRepository;
        this.inventoryRepository = inventoryRepository;
        this.doseEventRepository = doseEventRepository;
        this.deviceRepository = deviceRepository;
        this.healthLogRepository = healthLogRepository;
        this.auditService = auditService;
        //For alert integration
        this.storageAlertEvaluationService =
                storageAlertEvaluationService;
        this.inventoryAlertEvaluationService =
                inventoryAlertEvaluationService;
    }

    public void process(
            TelemetryPayloadDTO payload,
            String rawJson
    ) {
        process(payload, rawJson, null);
    }

    public void process(
            TelemetryPayloadDTO payload,
            String rawJson,
            String mqttTopic
    ) {
        if (payload == null) {
            throw new IllegalArgumentException(
                    "Telemetry payload is required"
            );
        }

        OffsetDateTime receivedAt = OffsetDateTime.now();
        OffsetDateTime eventTime =
                parseTimestamp(payload.getTimestamp(), receivedAt);
        Long patientId = getPatientId(payload);
        String sourceDeviceUid = resolveSourceDeviceUid(payload);
        Device sourceDevice =
                resolveSourceDevice(
                        payload,
                        sourceDeviceUid,
                        patientId,
                        receivedAt
                );
        String sourceEventId = trimToNull(payload.getEventId());

        if (sourceEventId != null
                && rawRepository.existsByDeviceUidAndSourceEventId(
                        sourceDeviceUid,
                        sourceEventId
                )) {
            System.out.println(
                    "Duplicate telemetry event skipped: "
                            + sourceEventId
            );
            auditService.logDuplicateMqttEvent(
                    patientId,
                    getDeviceId(sourceDevice),
                    sourceDeviceUid,
                    sourceEventId,
                    mqttTopic,
                    payload.getReplayedEvent()
            );
            return;
        }

        RawDeviceEvent rawEvent =
                saveRawEvent(
                        payload,
                        rawJson,
                        mqttTopic,
                        sourceDevice,
                        sourceDeviceUid,
                        patientId,
                        eventTime,
                        receivedAt
                );

        if (Boolean.TRUE.equals(payload.getReplayedEvent())) {
            auditService.logMqttReplayEvent(
                    patientId,
                    getDeviceId(sourceDevice),
                    rawEvent.getRawEventId(),
                    sourceDeviceUid,
                    sourceEventId,
                    rawEvent.getEventType(),
                    mqttTopic,
                    eventTime
            );
        }

        try {
            int savedRows = 0;

            savedRows += saveGlucose(
                    payload,
                    rawEvent,
                    patientId,
                    eventTime,
                    receivedAt
            );
            savedRows += saveStorage(
                    payload,
                    rawEvent,
                    patientId,
                    eventTime,
                    receivedAt
            );
            savedRows += saveInventory(
                    payload,
                    rawEvent,
                    patientId,
                    eventTime,
                    receivedAt
            );
            savedRows += saveDose(
                    payload,
                    rawEvent,
                    patientId,
                    eventTime,
                    receivedAt
            );
            savedRows += saveBatteryHealth(
                    payload,
                    rawEvent,
                    patientId,
                    eventTime,
                    receivedAt
            );

            rawEvent.setProcessingStatus(
                    savedRows > 0 ? "PROCESSED" : "IGNORED"
            );
            rawEvent.setProcessingError(null);
            rawRepository.save(rawEvent);

            System.out.println(
                    "Telemetry processed. Normalized rows saved: "
                            + savedRows
            );
        } catch (RuntimeException ex) {
            rawEvent.setProcessingStatus("FAILED");
            rawEvent.setProcessingError(
                    truncate(ex.getMessage(), 1000)
            );
            rawRepository.save(rawEvent);

            if (ex instanceof IllegalArgumentException) {
                auditService.logFailedPayloadValidation(
                        patientId,
                        getDeviceId(sourceDevice),
                        rawEvent.getRawEventId(),
                        sourceDeviceUid,
                        sourceEventId,
                        mqttTopic,
                        ex.getMessage(),
                        rawJson
                );
            }

            System.out.println(
                    "Telemetry processing failed: "
                            + ex.getMessage()
            );
            throw ex;
        }
    }

    private RawDeviceEvent saveRawEvent(
            TelemetryPayloadDTO payload,
            String rawJson,
            String mqttTopic,
            Device sourceDevice,
            String sourceDeviceUid,
            Long patientId,
            OffsetDateTime eventTime,
            OffsetDateTime receivedAt
    ) {
        RawDeviceEvent rawEvent = new RawDeviceEvent();

        rawEvent.setSourceEventId(
                trimToNull(payload.getEventId())
        );
        rawEvent.setDeviceUid(sourceDeviceUid);
        rawEvent.setDeviceId(getDeviceId(sourceDevice));
        rawEvent.setPatientId(patientId);
        rawEvent.setMqttTopic(mqttTopic);
        rawEvent.setEventType(normalizeEventType(payload));
        rawEvent.setEventTime(eventTime);
        rawEvent.setReceivedAt(receivedAt);
        rawEvent.setPayload(parsePayload(rawJson));
        rawEvent.setProcessingStatus("RECEIVED");

        RawDeviceEvent saved = rawRepository.save(rawEvent);
        System.out.println("Raw telemetry event saved");
        return saved;
    }

    private int saveGlucose(
            TelemetryPayloadDTO payload,
            RawDeviceEvent rawEvent,
            Long patientId,
            OffsetDateTime eventTime,
            OffsetDateTime createdAt
    ) {
        GlucoseDTO dto = payload.getGlucose();

        if (dto == null || dto.getValueMgDl() == null) {
            return 0;
        }

        Long normalizedPatientId =
                requirePatientId(patientId, "glucose");
        Device device =
                upsertDevice(
                        firstNonBlank(
                                dto.getDeviceUid(),
                                DEFAULT_GLUCOMETER_UID
                        ),
                        normalizedPatientId,
                        "GLUCOMETER",
                        "Dia-Smart Glucometer",
                        "BLE",
                        null,
                        createdAt
                );

        GlucoseReading glucose = new GlucoseReading();

        glucose.setPatientId(normalizedPatientId);
        glucose.setDeviceId(getDeviceId(device));
        glucose.setRawEventId(rawEvent.getRawEventId());
        glucose.setMeasuredAt(eventTime);
        glucose.setGlucoseValueMgDl(
                dto.getValueMgDl().doubleValue()
        );
        glucose.setSource(
                normalizeValue(
                        dto.getSource(),
                        GLUCOSE_SOURCES,
                        "BLE_GLUCOMETER"
                )
        );
        glucose.setMealContext(
                normalizeValue(
                        dto.getMealContext(),
                        MEAL_CONTEXTS,
                        "UNKNOWN"
                )
        );
        glucose.setGlucometerSequenceNumber(
                dto.getSequenceNumber()
        );
        glucose.setNotes(trimToNull(dto.getNotes()));
        glucose.setCreatedAt(createdAt);

        glucoseRepository.save(glucose);
        System.out.println("Glucose reading saved");
        return 1;
    }

    private int saveStorage(
            TelemetryPayloadDTO payload,
            RawDeviceEvent rawEvent,
            Long patientId,
            OffsetDateTime eventTime,
            OffsetDateTime createdAt
    ) {
        StorageTelemetryDTO dto = payload.getStorage();

        if (dto == null || !hasStorageValue(dto)) {
            return 0;
        }

        Long normalizedPatientId =
                requirePatientId(patientId, "storage");
        Device device =
                upsertDevice(
                        firstNonBlank(
                                dto.getDeviceUid(),
                                innerUnitDeviceUid(payload)
                        ),
                        normalizedPatientId,
                        "INNER_UNIT",
                        "Dia-Smart Inner Unit",
                        "ESP_NOW",
                        null,
                        createdAt
                );

        Double temperatureC =
                validateRange(
                        dto.getTemperatureC(),
                        -40.0,
                        80.0,
                        "storage.temperatureC"
                );
       
        String doorState =
                normalizeValue(
                        dto.getDoorStatus(),
                        DOOR_STATES,
                        "UNKNOWN"
                );
        String temperatureStatus =
                determineTemperatureStatus(
                        dto.getTemperatureStatus(),
                        dto.getTemperatureC()
                );

        StorageReading lastReading =
                storageRepository
                        .findTopByPatientIdOrderByMeasuredAtDesc(
                                normalizedPatientId
                        );

        if (lastReading != null
        && Objects.equals(
                lastReading.getDoorState(),
                doorState
        )
        && sameDouble(
                lastReading.getTemperatureC(),
                temperatureC
        )) {

    System.out.println(
            "Storage unchanged - skipped"
    );

    return 0;
}

        StorageReading storage = new StorageReading();

        storage.setPatientId(normalizedPatientId);
        storage.setDeviceId(getDeviceId(device));
        storage.setRawEventId(rawEvent.getRawEventId());
        storage.setMeasuredAt(eventTime);
        storage.setCreatedAt(createdAt);
        storage.setTemperatureC(temperatureC);
        storage.setDoorState(doorState);
        storage.setDoorOpenDurationSeconds(
                dto.getDoorOpenDurationSeconds()
        );
        storage.setTemperatureStatus(temperatureStatus);
        storage.setNotes(trimToNull(dto.getNotes()));

        storageRepository.save(storage);
        //For alert integration - evaluate storage alerts after saving new reading
                /*
        * Alert evaluation integration.
        *
        * Current phase:
        * synchronous alert evaluation
        * after successful persistence.
        *
        * Existing replay/value deduplication
        * already prevents most duplicate alerts.
        *
        * Future:
        * may move into async event pipeline.
        */
        storageAlertEvaluationService
        .evaluateStorageAlerts(storage);
        System.out.println("Storage reading saved");
        return 1;
    }

    private int saveInventory(
            TelemetryPayloadDTO payload,
            RawDeviceEvent rawEvent,
            Long patientId,
            OffsetDateTime eventTime,
            OffsetDateTime createdAt
    ) {
        InventoryTelemetryDTO dto = payload.getInventory();

        if (dto == null || !hasInventoryValue(dto)) {
            return 0;
        }

        Long normalizedPatientId =
                requirePatientId(patientId, "inventory");
        Device device =
                upsertDevice(
                        firstNonBlank(
                                dto.getDeviceUid(),
                                innerUnitDeviceUid(payload)
                        ),
                        normalizedPatientId,
                        "INNER_UNIT",
                        "Dia-Smart Inner Unit",
                        "ESP_NOW",
                        null,
                        createdAt
                );

        Double weightG =
                validateRange(
                        dto.getWeightG(),
                        0.0,
                        null,
                        "inventory.weightG"
                );
        Double estimatedUnitsRemaining =
                validateRange(
                        dto.getEstimatedUnitsRemaining(),
                        0.0,
                        null,
                        "inventory.estimatedUnitsRemaining"
                );
        Double estimatedRemainingPercent =
                validatePercent(
                        dto.getEstimatedRemainingPercent(),
                        "inventory.estimatedRemainingPercent"
                );
        String inventoryStatus =
                determineInventoryStatus(dto);

        InventoryReading lastReading =
                inventoryRepository
                        .findTopByPatientIdOrderByMeasuredAtDesc(
                                normalizedPatientId
                        );

        if (lastReading != null
                && Objects.equals(
                lastReading.getPenPresent(),
                dto.getPenPresent()
        )
                && Objects.equals(
                lastReading.getCartridgePresent(),
                dto.getCartridgePresent()
        )
                && sameDouble(lastReading.getWeightG(), weightG)
                && sameDouble(
                lastReading.getEstimatedUnitsRemaining(),
                estimatedUnitsRemaining
        )
                && sameDouble(
                lastReading.getEstimatedRemainingPercent(),
                estimatedRemainingPercent
        )
                && Objects.equals(
                lastReading.getInventoryStatus(),
                inventoryStatus
        )) {
            System.out.println(
                    "Inventory unchanged - skipped"
            );
            return 0;
        }

        InventoryReading inventory = new InventoryReading();

        inventory.setPatientId(normalizedPatientId);
        inventory.setDeviceId(getDeviceId(device));
        inventory.setRawEventId(rawEvent.getRawEventId());
        inventory.setMeasuredAt(eventTime);
        inventory.setCreatedAt(createdAt);
        inventory.setPenPresent(dto.getPenPresent());
        inventory.setCartridgePresent(
                dto.getCartridgePresent()
        );
        inventory.setWeightG(weightG);
        inventory.setEstimatedUnitsRemaining(
                estimatedUnitsRemaining
        );
        inventory.setEstimatedRemainingPercent(
                estimatedRemainingPercent
        );
        inventory.setInventoryStatus(inventoryStatus);
        inventory.setNotes(trimToNull(dto.getNotes()));

        inventoryRepository.save(inventory);
        /*
        * Alert evaluation integration.
        *
        * Current phase:
        * synchronous alert evaluation
        * after successful persistence.
        *
        * Existing replay/value deduplication
        * already prevents most duplicate alerts.
        *
        * Future:
        * may move into async event pipeline.
        */
        inventoryAlertEvaluationService
                .evaluateInventoryAlerts(inventory);
        System.out.println("Inventory reading saved");
        return 1;
    }

    private int saveDose(
            TelemetryPayloadDTO payload,
            RawDeviceEvent rawEvent,
            Long patientId,
            OffsetDateTime eventTime,
            OffsetDateTime createdAt
    ) {
        DoseDTO dto = payload.getDose();

        if (dto == null) {
            return 0;
        }

        Double doseUnits = getDoseUnits(dto);

        if (doseUnits == null) {
            return 0;
        }

        doseUnits =
                validateRange(
                        doseUnits,
                        0.01,
                        100.0,
                        "dose.doseUnits"
                );

        Long normalizedPatientId =
                requirePatientId(patientId, "dose");
        Device device =
                upsertDevice(
                        firstNonBlank(
                                dto.getDeviceUid(),
                                penUnitDeviceUid(payload)
                        ),
                        normalizedPatientId,
                        "DOSE_CAP",
                        "Dia-Smart Dose Cap",
                        "BLE",
                        null,
                        createdAt
                );

        DoseEvent doseEvent = new DoseEvent();

        doseEvent.setPatientId(normalizedPatientId);
        doseEvent.setDeviceId(getDeviceId(device));
        doseEvent.setRawEventId(rawEvent.getRawEventId());
        doseEvent.setInjectedAt(
                parseTimestamp(
                        dto.getInjectedAt(),
                        eventTime
                )
        );
        doseEvent.setDoseUnits(doseUnits);
        doseEvent.setDetectionMethod(
                normalizeValue(
                        dto.getDetectionMethod(),
                        DETECTION_METHODS,
                        "AS5600"
                )
        );
        doseEvent.setAngleDegrees(dto.getAngleDegrees());
        doseEvent.setConfidencePercent(
                validatePercent(
                        dto.getConfidencePercent(),
                        "dose.confidencePercent"
                )
        );
        doseEvent.setEventStatus(
                normalizeValue(
                        dto.getEventStatus(),
                        DOSE_STATUSES,
                        "CONFIRMED"
                )
        );
        doseEvent.setNotes(trimToNull(dto.getNotes()));
        doseEvent.setCreatedAt(createdAt);

        doseEventRepository.save(doseEvent);
        System.out.println("Dose event saved");
        return 1;
    }

    private int saveBatteryHealth(
            TelemetryPayloadDTO payload,
            RawDeviceEvent rawEvent,
            Long patientId,
            OffsetDateTime eventTime,
            OffsetDateTime createdAt
    ) {
        BatteryTelemetryDTO dto = payload.getBattery();

        if (dto == null) {
            return 0;
        }

        Long normalizedPatientId =
                requirePatientId(patientId, "battery");
        int savedRows = 0;

        savedRows += saveHealthLog(
                firstNonBlank(
                        dto.getInnerUnitDeviceUid(),
                        innerUnitDeviceUid(payload)
                ),
                normalizedPatientId,
                "INNER_UNIT",
                "Dia-Smart Inner Unit",
                "ESP_NOW",
                null,
                toDouble(dto.getInnerUnitPercent()),
                dto.getInnerUnitVoltageV(),
                false,
                dto,
                rawEvent,
                eventTime,
                createdAt
        );
        savedRows += saveHealthLog(
                firstNonBlank(
                        dto.getPenUnitDeviceUid(),
                        penUnitDeviceUid(payload)
                ),
                normalizedPatientId,
                "DOSE_CAP",
                "Dia-Smart Dose Cap",
                "BLE",
                null,
                toDouble(dto.getPenUnitPercent()),
                dto.getPenUnitVoltageV(),
                false,
                dto,
                rawEvent,
                eventTime,
                createdAt
        );
        savedRows += saveHealthLog(
                firstNonBlank(
                        dto.getOuterUnitDeviceUid(),
                        gatewayDeviceUid(payload)
                ),
                normalizedPatientId,
                "OUTER_GATEWAY",
                "Dia-Smart Outer Gateway",
                "MQTTS",
                getFirmwareVersion(payload),
                toDouble(dto.getOuterUnitPercent()),
                dto.getOuterUnitVoltageV(),
                true,
                dto,
                rawEvent,
                eventTime,
                createdAt
        );

        if (savedRows > 0) {
            System.out.println(
                    "Battery health logs saved: " + savedRows
            );
        }

        return savedRows;
    }

    private int saveHealthLog(
            String deviceUid,
            Long patientId,
            String deviceType,
            String deviceName,
            String communicationType,
            String firmwareVersion,
            Double batteryPercent,
            Double batteryVoltage,
            boolean includeGatewayDiagnostics,
            BatteryTelemetryDTO battery,
            RawDeviceEvent rawEvent,
            OffsetDateTime eventTime,
            OffsetDateTime createdAt
    ) {
        if (trimToNull(deviceUid) == null
                || !hasHealthValue(
                batteryPercent,
                batteryVoltage,
                includeGatewayDiagnostics,
                battery
        )) {
            return 0;
        }

        Device device =
                upsertDevice(
                        deviceUid,
                        patientId,
                        deviceType,
                        deviceName,
                        communicationType,
                        firmwareVersion,
                        createdAt
                );

        DeviceHealthLog healthLog = new DeviceHealthLog();

        String powerSource =
                normalizeValue(
                        battery.getPowerSource(),
                        POWER_SOURCES,
                        "UNKNOWN"
                );
        String healthStatus =
                determineHealthStatus(
                        battery.getStatus(),
                        batteryPercent
                );

        DeviceHealthLog lastLog =
                healthLogRepository
                        .findTopByDeviceIdOrderByMeasuredAtDesc(
                                device.getDeviceId()
                        );

        if (lastLog != null
                && sameDouble(
                lastLog.getBatteryPercent(),
                validatePercent(
                        batteryPercent,
                        "battery.percent"
                )
        )
                && sameDouble(
                lastLog.getBatteryVoltageV(),
                batteryVoltage
        )
                && Objects.equals(
                lastLog.getPowerSource(),
                powerSource
        )
                && Objects.equals(
                lastLog.getFirmwareVersion(),
                firmwareVersion
        )
                && Objects.equals(
                lastLog.getStatus(),
                healthStatus
        )
                && (!includeGatewayDiagnostics
                || (Objects.equals(
                lastLog.getWifiRssiDbm(),
                battery.getWifiRssiDbm()
        )
                && Objects.equals(
                lastLog.getBleRssiDbm(),
                battery.getBleRssiDbm()
        )
                && Objects.equals(
                lastLog.getFreeHeapBytes(),
                battery.getFreeHeapBytes()
        )))) {
            System.out.println(
                    "Battery unchanged - skipped for "
                            + deviceUid
            );
            return 0;
        }

        healthLog.setDeviceId(device.getDeviceId());
        healthLog.setRawEventId(rawEvent.getRawEventId());
        healthLog.setMeasuredAt(eventTime);
        healthLog.setCreatedAt(createdAt);
        healthLog.setBatteryPercent(
                validatePercent(
                        batteryPercent,
                        "battery.percent"
                )
        );
        healthLog.setBatteryVoltageV(batteryVoltage);
        healthLog.setPowerSource(powerSource);
        healthLog.setFirmwareVersion(firmwareVersion);
        healthLog.setOnline(true);
        healthLog.setStatus(healthStatus);

        if (includeGatewayDiagnostics) {
            healthLog.setWifiRssiDbm(
                    battery.getWifiRssiDbm()
            );
            healthLog.setBleRssiDbm(
                    battery.getBleRssiDbm()
            );
            healthLog.setFreeHeapBytes(
                    battery.getFreeHeapBytes()
            );
        }

        healthLogRepository.save(healthLog);
        return 1;
    }

    private Device resolveSourceDevice(
            TelemetryPayloadDTO payload,
            String sourceDeviceUid,
            Long patientId,
            OffsetDateTime lastSeenAt
    ) {
        if ("UNKNOWN_DEVICE".equals(sourceDeviceUid)) {
            return null;
        }

        String gatewayUid = gatewayDeviceUid(payload);
        boolean isGateway =
                gatewayUid != null
                        && gatewayUid.equals(sourceDeviceUid);

        return upsertDevice(
                sourceDeviceUid,
                patientId,
                isGateway ? "OUTER_GATEWAY" : "OTHER",
                isGateway
                        ? "Dia-Smart Outer Gateway"
                        : "Dia-Smart Device",
                isGateway ? "MQTTS" : "OTHER",
                isGateway ? getFirmwareVersion(payload) : null,
                lastSeenAt
        );
    }

    private Device upsertDevice(
            String deviceUid,
            Long patientId,
            String deviceType,
            String deviceName,
            String communicationType,
            String firmwareVersion,
            OffsetDateTime lastSeenAt
    ) {
        String normalizedUid = trimToNull(deviceUid);

        if (normalizedUid == null
                || "UNKNOWN_DEVICE".equals(normalizedUid)) {
            return null;
        }

        Device device =
                deviceRepository
                        .findByDeviceUid(normalizedUid)
                        .orElseGet(Device::new);

        device.setDeviceUid(normalizedUid);

        if (patientId != null) {
            device.setPatientId(patientId);
        }

        device.setDeviceType(
                normalizeValue(
                        deviceType,
                        DEVICE_TYPES,
                        "OTHER"
                )
        );
        device.setDeviceName(deviceName);
        device.setCommunicationType(
                normalizeValue(
                        communicationType,
                        COMMUNICATION_TYPES,
                        "OTHER"
                )
        );
        device.setFirmwareVersion(
                firstNonBlank(
                        firmwareVersion,
                        device.getFirmwareVersion()
                )
        );
        device.setLastSeenAt(lastSeenAt);
        device.setActive(true);

        return deviceRepository.save(device);
    }

    private String normalizeEventType(
            TelemetryPayloadDTO payload
    ) {
        String eventType =
                normalizeValue(
                        payload.getEventType(),
                        EVENT_TYPES,
                        null
                );

        if (eventType != null) {
            return eventType;
        }

        String trigger =
                normalizeValue(
                        payload.getTrigger(),
                        EVENT_TYPES,
                        null
                );

        if (trigger != null) {
            return trigger;
        }

        int sectionCount = 0;
        String detectedType = null;

        if (payload.getStorage() != null) {
            sectionCount++;
            detectedType = "STORAGE_READING";
        }

        if (payload.getInventory() != null) {
            sectionCount++;
            detectedType = "INVENTORY_READING";
        }

        if (payload.getGlucose() != null) {
            sectionCount++;
            detectedType = "GLUCOSE_READING";
        }

        if (payload.getDose() != null) {
            sectionCount++;
            detectedType = "DOSE_EVENT";
        }

        if (payload.getBattery() != null) {
            sectionCount++;
            detectedType = "DEVICE_HEALTH";
        }

        if (sectionCount > 1) {
            return "COMBINED_TELEMETRY";
        }

        return detectedType == null ? "UNKNOWN" : detectedType;
    }

    private String resolveSourceDeviceUid(
            TelemetryPayloadDTO payload
    ) {
        return firstNonBlank(
                gatewayDeviceUid(payload),
                storageDeviceUid(payload),
                inventoryDeviceUid(payload),
                glucoseDeviceUid(payload),
                doseDeviceUid(payload),
                batteryOuterDeviceUid(payload),
                batteryInnerDeviceUid(payload),
                batteryPenDeviceUid(payload),
                "UNKNOWN_DEVICE"
        );
    }

    private String innerUnitDeviceUid(
            TelemetryPayloadDTO payload
    ) {
        return firstNonBlank(
                storageDeviceUid(payload),
                inventoryDeviceUid(payload),
                batteryInnerDeviceUid(payload),
                DEFAULT_INNER_UNIT_UID
        );
    }

    private String penUnitDeviceUid(
            TelemetryPayloadDTO payload
    ) {
        return firstNonBlank(
                doseDeviceUid(payload),
                batteryPenDeviceUid(payload),
                DEFAULT_DOSE_CAP_UID
        );
    }

    private String gatewayDeviceUid(
            TelemetryPayloadDTO payload
    ) {
        return payload.getGateway() == null
                ? null
                : trimToNull(
                        payload.getGateway().getDeviceUid()
                );
    }

    private String getFirmwareVersion(
            TelemetryPayloadDTO payload
    ) {
        return payload.getGateway() == null
                ? null
                : trimToNull(
                        payload.getGateway()
                                .getFirmwareVersion()
                );
    }

    private String storageDeviceUid(
            TelemetryPayloadDTO payload
    ) {
        return payload.getStorage() == null
                ? null
                : trimToNull(
                        payload.getStorage().getDeviceUid()
                );
    }

    private String inventoryDeviceUid(
            TelemetryPayloadDTO payload
    ) {
        return payload.getInventory() == null
                ? null
                : trimToNull(
                        payload.getInventory().getDeviceUid()
                );
    }

    private String glucoseDeviceUid(
            TelemetryPayloadDTO payload
    ) {
        return payload.getGlucose() == null
                ? null
                : trimToNull(
                        payload.getGlucose().getDeviceUid()
                );
    }

    private String doseDeviceUid(
            TelemetryPayloadDTO payload
    ) {
        return payload.getDose() == null
                ? null
                : trimToNull(
                        payload.getDose().getDeviceUid()
                );
    }

    private String batteryOuterDeviceUid(
            TelemetryPayloadDTO payload
    ) {
        return payload.getBattery() == null
                ? null
                : trimToNull(
                        payload.getBattery()
                                .getOuterUnitDeviceUid()
                );
    }

    private String batteryInnerDeviceUid(
            TelemetryPayloadDTO payload
    ) {
        return payload.getBattery() == null
                ? null
                : trimToNull(
                        payload.getBattery()
                                .getInnerUnitDeviceUid()
                );
    }

    private String batteryPenDeviceUid(
            TelemetryPayloadDTO payload
    ) {
        return payload.getBattery() == null
                ? null
                : trimToNull(
                        payload.getBattery()
                                .getPenUnitDeviceUid()
                );
    }

    private Long getPatientId(
            TelemetryPayloadDTO payload
    ) {
        return payload.getPatient() == null
                ? null
                : payload.getPatient().getPatientId();
    }

    private Long requirePatientId(
            Long patientId,
            String section
    ) {
        if (patientId == null) {
            throw new IllegalArgumentException(
                    "patient.patientId is required for "
                            + section
                            + " telemetry"
            );
        }

        return patientId;
    }

    private Long getDeviceId(Device device) {
        return device == null ? null : device.getDeviceId();
    }

    private boolean hasStorageValue(
            StorageTelemetryDTO dto
    ) {
        return dto.getTemperatureC() != null
                || dto.getHumidityPercent() != null
                || dto.getDoorStatus() != null
                || dto.getDoorOpenDurationSeconds() != null;
    }

    private boolean hasInventoryValue(
            InventoryTelemetryDTO dto
    ) {
        return dto.getPenPresent() != null
                || dto.getCartridgePresent() != null
                || dto.getWeightG() != null
                || dto.getEstimatedUnitsRemaining() != null
                || dto.getEstimatedRemainingPercent() != null
                || dto.getInventoryStatus() != null;
    }

    private boolean hasHealthValue(
            Double batteryPercent,
            Double batteryVoltage,
            boolean includeGatewayDiagnostics,
            BatteryTelemetryDTO battery
    ) {
        return batteryPercent != null
                || batteryVoltage != null
                || (includeGatewayDiagnostics
                && (battery.getWifiRssiDbm() != null
                || battery.getBleRssiDbm() != null
                || battery.getFreeHeapBytes() != null));
    }

    private String determineTemperatureStatus(
            String suppliedStatus,
            Double temperatureC
    ) {
        String status =
                normalizeValue(
                        suppliedStatus,
                        TEMPERATURE_STATUSES,
                        null
                );

        if (status != null) {
            return status;
        }

        if (temperatureC == null) {
            return "UNKNOWN";
        }

        if (temperatureC < 2.0) {
            return "LOW";
        }

        if (temperatureC > 8.0) {
            return "HIGH";
        }

        return "SAFE";
    }

    private String determineInventoryStatus(
            InventoryTelemetryDTO dto
    ) {
        String status =
                normalizeValue(
                        dto.getInventoryStatus(),
                        INVENTORY_STATUSES,
                        null
                );

        if (status != null) {
            return status;
        }

        if (Boolean.FALSE.equals(dto.getPenPresent())
                || Boolean.FALSE.equals(
                dto.getCartridgePresent()
        )) {
            return "REMOVED";
        }

        Double percent = dto.getEstimatedRemainingPercent();

        if (percent != null) {
            if (percent <= 0.0) {
                return "EMPTY";
            }
            if (percent <= 10.0) {
                return "CRITICAL";
            }
            if (percent <= 20.0) {
                return "LOW";
            }
            return "OK";
        }

        Double weight = dto.getWeightG();

        if (weight != null && weight <= 0.0) {
            return "EMPTY";
        }

        return "UNKNOWN";
    }

    private String determineHealthStatus(
            String suppliedStatus,
            Double batteryPercent
    ) {
        String status =
                normalizeValue(
                        suppliedStatus,
                        HEALTH_STATUSES,
                        null
                );

        if (status != null) {
            return status;
        }

        if (batteryPercent != null && batteryPercent <= 20.0) {
            return "LOW_BATTERY";
        }

        return "ONLINE";
    }

    private Double getDoseUnits(DoseDTO dto) {
        if (dto.getDoseUnits() != null) {
            return dto.getDoseUnits();
        }

        return dto.getInsulinDoseUnits() == null
                ? null
                : dto.getInsulinDoseUnits().doubleValue();
    }

    private Double toDouble(Integer value) {
        return value == null ? null : value.doubleValue();
    }

    private boolean sameDouble(
            Double first,
            Double second
    ) {
        return Objects.equals(first, second);
    }

    private OffsetDateTime parseTimestamp(
            String value,
            OffsetDateTime fallback
    ) {
        String timestamp = trimToNull(value);

        if (timestamp == null) {
            return fallback;
        }

        try {
            return OffsetDateTime.parse(timestamp);
        } catch (DateTimeParseException ignored) {
            return fallback;
        }
    }

    private Map<String, Object> parsePayload(
            String rawJson
    ) {
        String payload = trimToNull(rawJson);

        if (payload == null) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(
                    payload,
                    new TypeReference<>() {
                    }
            );
        } catch (Exception ignored) {
            return Map.of("raw", payload);
        }
    }

    private Double validatePercent(
            Double value,
            String fieldName
    ) {
        return validateRange(value, 0.0, 100.0, fieldName);
    }

    private Double validateRange(
            Double value,
            Double min,
            Double max,
            String fieldName
    ) {
        if (value == null) {
            return null;
        }

        if (min != null && value < min) {
            throw new IllegalArgumentException(
                    fieldName + " must be >= " + min
            );
        }

        if (max != null && value > max) {
            throw new IllegalArgumentException(
                    fieldName + " must be <= " + max
            );
        }

        return value;
    }

    private String normalizeValue(
            String value,
            Set<String> allowedValues,
            String defaultValue
    ) {
        String normalized = trimToNull(value);

        if (normalized == null) {
            return defaultValue;
        }

        normalized =
                normalized
                        .trim()
                        .toUpperCase(Locale.ROOT)
                        .replace('-', '_')
                        .replace(' ', '_');

        return allowedValues.contains(normalized)
                ? normalized
                : defaultValue;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);

            if (normalized != null) {
                return normalized;
            }
        }

        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String truncate(String value, int maxLength) {
        String trimmed = trimToNull(value);

        if (trimmed == null || trimmed.length() <= maxLength) {
            return trimmed;
        }

        return trimmed.substring(0, maxLength);
    }
}
