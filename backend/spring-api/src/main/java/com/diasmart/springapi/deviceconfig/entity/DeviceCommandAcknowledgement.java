package com.diasmart.springapi.deviceconfig.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "device_command_acknowledgements",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_device_command_ack_dedup", columnNames = "ack_deduplication_key")
        }
)
public class DeviceCommandAcknowledgement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "acknowledgement_id")
    private Long acknowledgementId;

    @Column(name = "command_id", nullable = false)
    private Long commandId;

    @Column(name = "command_uid", length = 80)
    private String commandUid;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "configuration_version")
    private Integer configurationVersion;

    @Column(name = "reporting_outer_device_uid", length = 80)
    private String reportingOuterDeviceUid;

    @Column(name = "payload_outer_device_uid", length = 80)
    private String payloadOuterDeviceUid;

    @Column(name = "ack_uid", length = 120)
    private String ackUid;

    @Column(name = "ack_deduplication_key", nullable = false, length = 200)
    private String ackDeduplicationKey;

    @Column(name = "ack_status", length = 20)
    private String ackStatus;

    @Column(name = "processing_result", length = 60)
    private String processingResult;

    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;

    @Column(name = "device_timestamp")
    private OffsetDateTime deviceTimestamp;

    @Column(name = "acknowledged_at", updatable = false)
    private OffsetDateTime acknowledgedAt;

    public DeviceCommandAcknowledgement() {
    }

    @PrePersist
    protected void onCreate() {
        acknowledgedAt = OffsetDateTime.now();
    }

    public Long getAcknowledgementId() {
        return acknowledgementId;
    }

    public void setAcknowledgementId(Long acknowledgementId) {
        this.acknowledgementId = acknowledgementId;
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

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getConfigurationVersion() {
        return configurationVersion;
    }

    public void setConfigurationVersion(Integer configurationVersion) {
        this.configurationVersion = configurationVersion;
    }

    public String getReportingOuterDeviceUid() {
        return reportingOuterDeviceUid;
    }

    public void setReportingOuterDeviceUid(String reportingOuterDeviceUid) {
        this.reportingOuterDeviceUid = reportingOuterDeviceUid;
    }

    public String getPayloadOuterDeviceUid() {
        return payloadOuterDeviceUid;
    }

    public void setPayloadOuterDeviceUid(String payloadOuterDeviceUid) {
        this.payloadOuterDeviceUid = payloadOuterDeviceUid;
    }

    public String getAckUid() {
        return ackUid;
    }

    public void setAckUid(String ackUid) {
        this.ackUid = ackUid;
    }

    public String getAckDeduplicationKey() {
        return ackDeduplicationKey;
    }

    public void setAckDeduplicationKey(String ackDeduplicationKey) {
        this.ackDeduplicationKey = ackDeduplicationKey;
    }

    public String getAckStatus() {
        return ackStatus;
    }

    public void setAckStatus(String ackStatus) {
        this.ackStatus = ackStatus;
    }

    public String getProcessingResult() {
        return processingResult;
    }

    public void setProcessingResult(String processingResult) {
        this.processingResult = processingResult;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public OffsetDateTime getDeviceTimestamp() {
        return deviceTimestamp;
    }

    public void setDeviceTimestamp(OffsetDateTime deviceTimestamp) {
        this.deviceTimestamp = deviceTimestamp;
    }

    public OffsetDateTime getAcknowledledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(OffsetDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }
}
