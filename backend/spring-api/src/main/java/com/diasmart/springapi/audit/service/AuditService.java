package com.diasmart.springapi.audit.service;

import com.diasmart.springapi.audit.entity.AuditLog;
import com.diasmart.springapi.audit.repository.AuditLogRepository;
import com.diasmart.springapi.devices.entity.Device;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuditService {

    public static final String DEVICE_REGISTERED =
            "DEVICE_REGISTERED";
    public static final String DEVICE_ASSIGNED =
            "DEVICE_ASSIGNED";
    public static final String MQTT_REPLAY_EVENT =
            "MQTT_REPLAY_EVENT";
    public static final String DUPLICATE_MQTT_EVENT =
            "DUPLICATE_MQTT_EVENT";
    public static final String NOTIFICATION_DELIVERY =
            "NOTIFICATION_DELIVERY";
    public static final String NOTIFICATION_PREFERENCES_UPDATED =
            "NOTIFICATION_PREFERENCES_UPDATED";
    public static final String FAILED_PAYLOAD_VALIDATION =
            "FAILED_PAYLOAD_VALIDATION";

    private static final int MAX_DETAIL_TEXT_LENGTH = 2000;

    private final AuditLogRepository auditLogRepository;

    public AuditService(
            AuditLogRepository auditLogRepository
    ) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(
            Long userId,
            Long patientId,
            String actionType,
            String entityType,
            Long entityId,
            String ipAddress,
            Map<String, Object> details
    ) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setPatientId(patientId);
            auditLog.setActionType(actionType);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setIpAddress(ipAddress);
            auditLog.setDetails(details);

            auditLogRepository.save(auditLog);
        } catch (RuntimeException ex) {
            System.out.println(
                    "Audit log write failed: "
                            + ex.getMessage()
            );
        }
    }

    public void logDeviceRegistration(Device device) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("deviceUid", device.getDeviceUid());
        details.put("deviceType", device.getDeviceType());
        details.put("deviceName", device.getDeviceName());
        details.put("firmwareVersion", device.getFirmwareVersion());

        record(
                null,
                device.getPatientId(),
                DEVICE_REGISTERED,
                "DEVICE",
                device.getDeviceId(),
                null,
                details
        );
    }

    public void logDeviceAssignment(
            Device device,
            Long previousPatientId,
            Long newPatientId
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("deviceUid", device.getDeviceUid());
        details.put("previousPatientId", previousPatientId);
        details.put("newPatientId", newPatientId);

        record(
                null,
                newPatientId,
                DEVICE_ASSIGNED,
                "DEVICE",
                device.getDeviceId(),
                null,
                details
        );
    }

    public void logMqttReplayEvent(
            Long patientId,
            Long deviceId,
            Long rawEventId,
            String deviceUid,
            String eventId,
            String eventType,
            String mqttTopic,
            OffsetDateTime eventTime
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("rawEventId", rawEventId);
        details.put("deviceUid", deviceUid);
        details.put("eventId", eventId);
        details.put("eventType", eventType);
        details.put("mqttTopic", mqttTopic);
        details.put("eventTime", eventTime);

        record(
                null,
                patientId,
                MQTT_REPLAY_EVENT,
                "DEVICE",
                deviceId,
                null,
                details
        );
    }

    public void logDuplicateMqttEvent(
            Long patientId,
            Long deviceId,
            String deviceUid,
            String eventId,
            String mqttTopic,
            Boolean replayedEvent
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("deviceUid", deviceUid);
        details.put("eventId", eventId);
        details.put("mqttTopic", mqttTopic);
        details.put("replayedEvent", replayedEvent);

        record(
                null,
                patientId,
                DUPLICATE_MQTT_EVENT,
                "DEVICE",
                deviceId,
                null,
                details
        );
    }

    public void logFailedPayloadValidation(
            Long patientId,
            Long deviceId,
            Long rawEventId,
            String deviceUid,
            String eventId,
            String mqttTopic,
            String errorMessage,
            String payload
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("rawEventId", rawEventId);
        details.put("deviceUid", deviceUid);
        details.put("eventId", eventId);
        details.put("mqttTopic", mqttTopic);
        details.put(
                "errorMessage",
                truncate(errorMessage, MAX_DETAIL_TEXT_LENGTH)
        );
        details.put(
                "payload",
                truncate(payload, MAX_DETAIL_TEXT_LENGTH)
        );

        record(
                null,
                patientId,
                FAILED_PAYLOAD_VALIDATION,
                deviceId == null ? "MQTT_MESSAGE" : "DEVICE",
                deviceId,
                null,
                details
        );
    }

    public void logNotificationDelivery(
            Long userId,
            Long patientId,
            Long notificationLogId,
            String channel,
            String deliveryStatus,
            String destination,
            String errorMessage
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("channel", channel);
        details.put("deliveryStatus", deliveryStatus);
        details.put("destination", destination);
        details.put(
                "errorMessage",
                truncate(errorMessage, MAX_DETAIL_TEXT_LENGTH)
        );

        record(
                userId,
                patientId,
                NOTIFICATION_DELIVERY,
                "NOTIFICATION_LOG",
                notificationLogId,
                null,
                details
        );
    }

    public long countDuplicateEventsForDevice(Long deviceId) {
        return auditLogRepository
                .countByActionTypeAndEntityTypeAndEntityId(
                        DUPLICATE_MQTT_EVENT,
                        "DEVICE",
                        deviceId
                );
    }

    private String truncate(
            String value,
            int maxLength
    ) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}
