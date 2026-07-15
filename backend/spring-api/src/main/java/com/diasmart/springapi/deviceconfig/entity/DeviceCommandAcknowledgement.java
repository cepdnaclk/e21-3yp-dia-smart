package com.diasmart.springapi.deviceconfig.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "device_command_acknowledgements")
public class DeviceCommandAcknowledgement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "acknowledgement_id")
    private Long acknowledgementId;

    @Column(name = "command_id", nullable = false)
    private Long commandId;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "ack_status", length = 20)
    private String ackStatus;

    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;

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

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getAckStatus() {
        return ackStatus;
    }

    public void setAckStatus(String ackStatus) {
        this.ackStatus = ackStatus;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public OffsetDateTime getAcknowledledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(OffsetDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }
}
