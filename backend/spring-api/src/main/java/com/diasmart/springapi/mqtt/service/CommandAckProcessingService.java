package com.diasmart.springapi.mqtt.service;

import com.diasmart.springapi.deviceconfig.entity.DeviceCommand;
import com.diasmart.springapi.deviceconfig.entity.DeviceCommandAcknowledgement;
import com.diasmart.springapi.deviceconfig.entity.DeviceConfiguration;
import com.diasmart.springapi.deviceconfig.repository.DeviceCommandAcknowledgementRepository;
import com.diasmart.springapi.deviceconfig.repository.DeviceCommandRepository;
import com.diasmart.springapi.deviceconfig.repository.DeviceConfigurationRepository;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.mqtt.dto.CommandAckDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class CommandAckProcessingService {

    private static final String COMMAND_TYPE_WIFI_CONFIGURATION = "WIFI_CONFIGURATION";
    private static final int MAX_ACK_MESSAGE_LENGTH = 500;

    private final DeviceCommandRepository commandRepository;
    private final DeviceCommandAcknowledgementRepository ackRepository;
    private final DeviceConfigurationRepository configRepository;
    private final DeviceRepository deviceRepository;

    public CommandAckProcessingService(
            DeviceCommandRepository commandRepository,
            DeviceCommandAcknowledgementRepository ackRepository,
            DeviceConfigurationRepository configRepository,
            DeviceRepository deviceRepository) {
        this.commandRepository = commandRepository;
        this.ackRepository = ackRepository;
        this.configRepository = configRepository;
        this.deviceRepository = deviceRepository;
    }

    @Transactional
    public void processAck(CommandAckDTO ackDto) {
        processAck(ackDto, null);
    }

    @Transactional
    public void processAck(CommandAckDTO ackDto, String topicOuterDeviceUid) {
        if (ackDto.getCommandId() == null || ackDto.getCommandId().isBlank()) {
            System.out.println("Command ACK received without commandId. Ignoring.");
            return;
        }

        DeviceCommand command = findCommand(ackDto.getCommandId())
                .orElse(null);

        if (command == null) {
            System.out.println("Command not found for ACK commandId: " + ackDto.getCommandId());
            return;
        }

        String reportingOuterUid = normalizeBlank(topicOuterDeviceUid);
        String payloadOuterUid = firstNonBlank(ackDto.getOuterDeviceUid(), ackDto.getOuterDeviceId());
        CommandAckStatus ackStatus = CommandAckStatus.fromFirmware(ackDto.getStatus());
        OffsetDateTime deviceTimestamp = ackDto.getTimestamp() != null
                ? ackDto.getTimestamp().atOffset(ZoneOffset.UTC)
                : null;
        String dedupKey = buildDeduplicationKey(command, ackDto, ackStatus, reportingOuterUid, deviceTimestamp);

        if (ackRepository.existsByAckDeduplicationKey(dedupKey)) {
            return;
        }

        AckValidation validation = validateWifiAck(command, ackDto, ackStatus, reportingOuterUid, payloadOuterUid);
        saveAcknowledgement(command, ackDto, ackStatus, reportingOuterUid, payloadOuterUid, deviceTimestamp, dedupKey, validation);

        if (!validation.accepted()) {
            System.out.println("WiFi command ACK ignored: " + validation.processingResult());
            return;
        }

        applyAcceptedAck(command, validation.configuration(), ackStatus, deviceTimestamp);
    }

    private AckValidation validateWifiAck(
            DeviceCommand command,
            CommandAckDTO ackDto,
            CommandAckStatus ackStatus,
            String reportingOuterUid,
            String payloadOuterUid
    ) {
        if (!COMMAND_TYPE_WIFI_CONFIGURATION.equals(command.getCommandType())) {
            return AckValidation.rejected("COMMAND_TYPE_MISMATCH", null);
        }

        String ackCommandType = normalizeBlank(ackDto.getCommandType());
        if (ackCommandType != null && !COMMAND_TYPE_WIFI_CONFIGURATION.equals(ackCommandType)) {
            return AckValidation.rejected("ACK_COMMAND_TYPE_MISMATCH", null);
        }

        if (reportingOuterUid == null) {
            return AckValidation.rejected("REPORTING_OUTER_UID_MISSING", null);
        }

        Device outerDevice = deviceRepository.findById(command.getDeviceId()).orElse(null);
        if (outerDevice == null || outerDevice.getDeviceUid() == null || !outerDevice.getDeviceUid().equals(reportingOuterUid)) {
            return AckValidation.rejected("REPORTING_OUTER_UID_MISMATCH", null);
        }

        if (payloadOuterUid != null && !payloadOuterUid.equals(reportingOuterUid)) {
            return AckValidation.rejected("PAYLOAD_OUTER_UID_MISMATCH", null);
        }

        if (command.getDeviceConfigurationId() == null || command.getConfigurationVersion() == null) {
            return AckValidation.rejected("COMMAND_CONFIGURATION_REFERENCE_MISSING", null);
        }

        Optional<DeviceConfiguration> configOptional = configRepository.findByConfigurationId(command.getDeviceConfigurationId());
        if (configOptional.isEmpty()) {
            return AckValidation.rejected("CONFIGURATION_NOT_FOUND", null);
        }

        DeviceConfiguration config = configOptional.get();
        if (!command.getDeviceId().equals(config.getOuterDeviceId())) {
            return AckValidation.rejected("CONFIGURATION_DEVICE_MISMATCH", config);
        }

        if (ackDto.getConfigurationVersion() == null) {
            return AckValidation.rejected("ACK_CONFIGURATION_VERSION_MISSING", config);
        }

        if (!ackDto.getConfigurationVersion().equals(command.getConfigurationVersion())) {
            return AckValidation.rejected("ACK_CONFIGURATION_VERSION_MISMATCH", config);
        }

        if (!command.getConfigurationVersion().equals(config.getConfigurationVersion())) {
            return AckValidation.rejected("COMMAND_SUPERSEDED", config);
        }

        if ("EXPIRED".equals(normalizeStatus(command.getCommandStatus()))) {
            return AckValidation.rejected("COMMAND_SUPERSEDED", config);
        }

        if (!ackStatus.canTransitionFrom(command.getCommandStatus())) {
            return AckValidation.rejected("STALE_STATUS_TRANSITION", config);
        }

        return AckValidation.accepted(config);
    }

    private void saveAcknowledgement(
            DeviceCommand command,
            CommandAckDTO ackDto,
            CommandAckStatus ackStatus,
            String reportingOuterUid,
            String payloadOuterUid,
            OffsetDateTime deviceTimestamp,
            String dedupKey,
            AckValidation validation
    ) {
        DeviceCommandAcknowledgement ack = new DeviceCommandAcknowledgement();
        ack.setCommandId(command.getCommandId());
        ack.setCommandUid(command.getCommandUid());
        ack.setDeviceId(command.getDeviceId());
        ack.setConfigurationVersion(ackDto.getConfigurationVersion());
        ack.setReportingOuterDeviceUid(reportingOuterUid);
        ack.setPayloadOuterDeviceUid(payloadOuterUid);
        ack.setAckUid(firstNonBlank(ackDto.getAcknowledgementId(), ackDto.getAckId()));
        ack.setAckDeduplicationKey(dedupKey);
        ack.setAckStatus(ackStatus.name());
        ack.setProcessingResult(validation.processingResult());
        ack.setResponseMessage(truncate(ackDto.getMessage(), MAX_ACK_MESSAGE_LENGTH));
        ack.setDeviceTimestamp(deviceTimestamp);
        ackRepository.save(ack);
    }

    private void applyAcceptedAck(
            DeviceCommand command,
            DeviceConfiguration config,
            CommandAckStatus ackStatus,
            OffsetDateTime deviceTimestamp
    ) {
        OffsetDateTime acknowledgedAt = deviceTimestamp != null ? deviceTimestamp : OffsetDateTime.now(ZoneOffset.UTC);
        command.setCommandStatus(ackStatus.commandStatus());
        command.setAcknowledgedAt(acknowledgedAt);
        if (ackStatus == CommandAckStatus.FAILED
                || ackStatus == CommandAckStatus.REJECTED
                || ackStatus == CommandAckStatus.ROLLED_BACK) {
            command.setCompletedAt(acknowledgedAt);
            command.setLastError("OUTER_" + ackStatus.name());
        }
        commandRepository.save(command);

        config.setOuterUnitStatus(ackStatus.name());
        if (ackStatus == CommandAckStatus.APPLIED) {
            config.setMqttStatus("CONNECTED");
            if ("CONNECTED".equals(config.getInnerUnitStatus())) {
                markProvisioningSucceeded(config, command, acknowledgedAt);
            } else {
                config.setConfigurationStatus("APPLYING");
            }
        } else if (ackStatus == CommandAckStatus.FAILED
                || ackStatus == CommandAckStatus.REJECTED
                || ackStatus == CommandAckStatus.ROLLED_BACK) {
            config.setConfigurationStatus("FAILED");
            config.setMqttStatus("FAILED");
            config.setProvisioningFailureCode("OUTER_" + ackStatus.name());
            config.setProvisioningFailureMessage("Outer Unit reported WiFi command " + ackStatus.name());
            config.setProvisioningCompletedAt(acknowledgedAt);
            if (ackStatus == CommandAckStatus.ROLLED_BACK) {
                config.setRollbackStatus("ROLLED_BACK");
            }
        } else {
            config.setConfigurationStatus(ackStatus.name());
            if (ackStatus == CommandAckStatus.APPLYING) {
                config.setMqttStatus("RECONNECTING");
            }
        }
        configRepository.save(config);
    }

    private void markProvisioningSucceeded(DeviceConfiguration config, DeviceCommand command, OffsetDateTime completedAt) {
        command.setCompletedAt(completedAt);
        command.setLastError(null);
        commandRepository.save(command);

        config.setConfigurationStatus("APPLIED");
        config.setProvisioningCompletedAt(completedAt);
        config.setProvisioningFailureCode(null);
        config.setProvisioningFailureMessage(null);
        config.setRollbackStatus("NOT_REQUIRED");
        config.setLastSyncedAt(completedAt);
        config.setLastSuccessfulConfigurationId(config.getConfigurationId());
        config.setLastSuccessfulConfigurationVersion(config.getConfigurationVersion());
        config.setLastSuccessfulAt(completedAt);
    }

    private java.util.Optional<DeviceCommand> findCommand(String commandId) {
        return commandRepository.findByCommandUid(commandId)
                .or(() -> parseNumericId(commandId).flatMap(commandRepository::findById));
    }

    private java.util.Optional<Long> parseNumericId(String commandId) {
        try {
            if (commandId.startsWith("CMD-")) {
                return java.util.Optional.of(Long.parseLong(commandId.substring(4)));
            }
            return java.util.Optional.of(Long.parseLong(commandId));
        } catch (NumberFormatException ex) {
            return java.util.Optional.empty();
        }
    }

    private String buildDeduplicationKey(
            DeviceCommand command,
            CommandAckDTO ackDto,
            CommandAckStatus ackStatus,
            String reportingOuterUid,
            OffsetDateTime deviceTimestamp
    ) {
        String firmwareAckId = firstNonBlank(ackDto.getAcknowledgementId(), ackDto.getAckId());
        if (firmwareAckId != null) {
            return "FW|" + command.getCommandUid() + "|" + firmwareAckId;
        }

        return "AUTO|"
                + command.getCommandUid() + "|"
                + ackStatus.name() + "|"
                + nullSafe(ackDto.getConfigurationVersion()) + "|"
                + nullSafe(reportingOuterUid) + "|"
                + nullSafe(deviceTimestamp);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String firstNonBlank(String first, String second) {
        String firstValue = normalizeBlank(first);
        return firstValue != null ? firstValue : normalizeBlank(second);
    }

    private String nullSafe(Object value) {
        return value == null ? "null" : value.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record AckValidation(boolean accepted, String processingResult, DeviceConfiguration configuration) {
        static AckValidation accepted(DeviceConfiguration configuration) {
            return new AckValidation(true, "ACCEPTED", configuration);
        }

        static AckValidation rejected(String processingResult, DeviceConfiguration configuration) {
            return new AckValidation(false, processingResult, configuration);
        }
    }
}
