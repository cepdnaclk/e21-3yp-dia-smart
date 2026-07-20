package com.diasmart.springapi.mqtt.service;

import com.diasmart.springapi.deviceconfig.entity.DeviceCommand;
import com.diasmart.springapi.deviceconfig.entity.DeviceCommandAcknowledgement;
import com.diasmart.springapi.deviceconfig.repository.DeviceCommandAcknowledgementRepository;
import com.diasmart.springapi.deviceconfig.repository.DeviceCommandRepository;
import com.diasmart.springapi.deviceconfig.repository.DeviceConfigurationRepository;
import com.diasmart.springapi.mqtt.dto.CommandAckDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class CommandAckProcessingService {

    private final DeviceCommandRepository commandRepository;
    private final DeviceCommandAcknowledgementRepository ackRepository;
    private final DeviceConfigurationRepository configRepository;

    public CommandAckProcessingService(
            DeviceCommandRepository commandRepository,
            DeviceCommandAcknowledgementRepository ackRepository,
            DeviceConfigurationRepository configRepository) {
        this.commandRepository = commandRepository;
        this.ackRepository = ackRepository;
        this.configRepository = configRepository;
    }

    @Transactional
    public void processAck(CommandAckDTO ackDto) {
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

        String status = normalizeStatus(ackDto.getStatus());
        command.setCommandStatus("REJECTED".equals(status) ? "FAILED" : status);
        command.setAcknowledgedAt(ackDto.getTimestamp() != null 
                ? ackDto.getTimestamp().atOffset(ZoneOffset.UTC) 
                : OffsetDateTime.now());
        commandRepository.save(command);

        DeviceCommandAcknowledgement ack = new DeviceCommandAcknowledgement();
        ack.setCommandId(command.getCommandId());
        ack.setCommandUid(command.getCommandUid());
        ack.setDeviceId(command.getDeviceId());
        ack.setAckStatus(status);
        ack.setResponseMessage(ackDto.getMessage());
        ackRepository.save(ack);

        if (isWifiConfigurationCommand(command.getCommandType())) {
            configRepository.findByOuterDeviceId(command.getDeviceId()).ifPresent(config -> {
                if (ackDto.getConfigurationVersion() != null
                        && !ackDto.getConfigurationVersion().equals(config.getConfigurationVersion())) {
                    System.out.println("ACK configuration version mismatch. Expected " + config.getConfigurationVersion() + " but got " + ackDto.getConfigurationVersion());
                    return;
                }

                config.setOuterUnitStatus(status);
                if ("APPLIED".equals(status)) {
                    config.setConfigurationStatus("APPLIED");
                    config.setLastSyncedAt(OffsetDateTime.now());
                    configRepository.save(config);
                    System.out.println("DeviceConfiguration status updated to APPLIED for deviceId: " + command.getDeviceId());
                } else if ("FAILED".equals(status) || "REJECTED".equals(status)) {
                    config.setConfigurationStatus("FAILED");
                    configRepository.save(config);
                } else {
                    config.setConfigurationStatus(status);
                    configRepository.save(config);
                }
            });
        }
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

    private boolean isWifiConfigurationCommand(String commandType) {
        return "WIFI_CONFIGURATION".equals(commandType) || "CONFIG_UPDATE".equals(commandType);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "FAILED";
        }
        return status.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
