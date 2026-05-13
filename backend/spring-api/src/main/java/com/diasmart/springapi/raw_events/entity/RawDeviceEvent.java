package com.diasmart.springapi.raw_events.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "raw_device_events")
public class RawDeviceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "raw_event_id")
    private Long rawEventId;

    @Column(name = "event_uuid", insertable = false, updatable = false)
    private UUID eventUuid;

    @Column(name = "source_event_id")
    private String sourceEventId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "device_uid")
    private String deviceUid;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "mqtt_topic")
    private String mqttTopic;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "event_time")
    private OffsetDateTime eventTime;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "received_at")
    private OffsetDateTime receivedAt;

    @Column(name = "processing_status")
    private String processingStatus;

    @Column(name = "processing_error")
    private String processingError;

    // =========================
    // GETTERS
    // =========================

    public Long getRawEventId() {
        return rawEventId;
    }

    public UUID getEventUuid() {
        return eventUuid;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public String getEventId() {
        return sourceEventId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public String getDeviceUid() {
        return deviceUid;
    }

    public Long getPatientId() {
        return patientId;
    }

    public String getMqttTopic() {
        return mqttTopic;
    }

    public String getEventType() {
        return eventType;
    }

    public OffsetDateTime getEventTime() {
        return eventTime;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public String getProcessingError() {
        return processingError;
    }

    // =========================
    // SETTERS
    // =========================

    public void setRawEventId(Long rawEventId) {
        this.rawEventId = rawEventId;
    }

    public void setEventUuid(UUID eventUuid) {
        this.eventUuid = eventUuid;
    }

    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public void setEventId(String eventId) {
        this.sourceEventId = eventId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public void setDeviceUid(String deviceUid) {
        this.deviceUid = deviceUid;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public void setMqttTopic(String mqttTopic) {
        this.mqttTopic = mqttTopic;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setEventTime(OffsetDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public void setReceivedAt(
            OffsetDateTime receivedAt
    ) {
        this.receivedAt = receivedAt;
    }

    public void setProcessingStatus(
            String processingStatus
    ) {
        this.processingStatus = processingStatus;
    }

    public void setProcessingError(
            String processingError
    ) {
        this.processingError = processingError;
    }
}
