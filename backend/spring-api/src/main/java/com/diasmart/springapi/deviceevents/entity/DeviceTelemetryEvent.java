package com.diasmart.springapi.deviceevents.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "device_telemetry_events")
public class DeviceTelemetryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "telemetry_event_id")
    private Long telemetryEventId;

    @Column(name = "event_id", nullable = false, unique = true, length = 120)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "outer_device_id")
    private Long outerDeviceId;

    @Column(name = "outer_device_uid", nullable = false, length = 80)
    private String outerDeviceUid;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "care_plan_version")
    private Integer carePlanVersion;

    @Column(name = "schedule_external_id", length = 80)
    private String scheduleExternalId;

    @Column(name = "command_id")
    private Long commandId;

    @Column(name = "command_uid", length = 80)
    private String commandUid;

    @Column(name = "device_configuration_id")
    private Long deviceConfigurationId;

    @Column(name = "configuration_version")
    private Integer configurationVersion;

    @Column(name = "inner_device_id")
    private Long innerDeviceId;

    @Column(name = "inner_device_uid", length = 80)
    private String innerDeviceUid;

    @Column(name = "event_timestamp")
    private OffsetDateTime eventTimestamp;

    @Column(name = "mqtt_topic", length = 255)
    private String mqttTopic;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "JSONB")
    private String payload;

    @Column(name = "processing_status", nullable = false, length = 20)
    private String processingStatus;

    @Column(name = "ack_status", nullable = false, length = 20)
    private String ackStatus;

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    @Column(name = "processing_result", length = 60)
    private String processingResult;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = OffsetDateTime.now();
        }
        if (processingStatus == null) {
            processingStatus = "RECEIVED";
        }
        if (ackStatus == null) {
            ackStatus = "REJECTED";
        }
    }

    public Long getTelemetryEventId() {
        return telemetryEventId;
    }

    public void setTelemetryEventId(Long telemetryEventId) {
        this.telemetryEventId = telemetryEventId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getOuterDeviceId() {
        return outerDeviceId;
    }

    public void setOuterDeviceId(Long outerDeviceId) {
        this.outerDeviceId = outerDeviceId;
    }

    public String getOuterDeviceUid() {
        return outerDeviceUid;
    }

    public void setOuterDeviceUid(String outerDeviceUid) {
        this.outerDeviceUid = outerDeviceUid;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Integer getCarePlanVersion() {
        return carePlanVersion;
    }

    public void setCarePlanVersion(Integer carePlanVersion) {
        this.carePlanVersion = carePlanVersion;
    }

    public String getScheduleExternalId() {
        return scheduleExternalId;
    }

    public void setScheduleExternalId(String scheduleExternalId) {
        this.scheduleExternalId = scheduleExternalId;
    }

    public Long getCommandId() {
        return commandId;
    }

    public void setCommandId(Long commandId) {
        this.commandId = commandId;
    }

    public String getCommandUid() {
        return commandUid;
    }

    public void setCommandUid(String commandUid) {
        this.commandUid = commandUid;
    }

    public Long getDeviceConfigurationId() {
        return deviceConfigurationId;
    }

    public void setDeviceConfigurationId(Long deviceConfigurationId) {
        this.deviceConfigurationId = deviceConfigurationId;
    }

    public Integer getConfigurationVersion() {
        return configurationVersion;
    }

    public void setConfigurationVersion(Integer configurationVersion) {
        this.configurationVersion = configurationVersion;
    }

    public Long getInnerDeviceId() {
        return innerDeviceId;
    }

    public void setInnerDeviceId(Long innerDeviceId) {
        this.innerDeviceId = innerDeviceId;
    }

    public String getInnerDeviceUid() {
        return innerDeviceUid;
    }

    public void setInnerDeviceUid(String innerDeviceUid) {
        this.innerDeviceUid = innerDeviceUid;
    }

    public OffsetDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(OffsetDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public String getMqttTopic() {
        return mqttTopic;
    }

    public void setMqttTopic(String mqttTopic) {
        this.mqttTopic = mqttTopic;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
    }

    public String getAckStatus() {
        return ackStatus;
    }

    public void setAckStatus(String ackStatus) {
        this.ackStatus = ackStatus;
    }

    public String getProcessingError() {
        return processingError;
    }

    public void setProcessingError(String processingError) {
        this.processingError = processingError;
    }

    public String getProcessingResult() {
        return processingResult;
    }

    public void setProcessingResult(String processingResult) {
        this.processingResult = processingResult;
    }

    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(OffsetDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(OffsetDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
