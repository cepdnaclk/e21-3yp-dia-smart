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
        if (ackDto.getCommandId() == null) {
            System.out.println("Command ACK received without commandId. Ignoring.");
            return;
        }

        DeviceCommand command = commandRepository.findById(ackDto.getCommandId())
                .orElse(null);

        if (command == null) {
            System.out.println("Command not found for ACK commandId: " + ackDto.getCommandId());
            return;
        }

        command.setCommandStatus(ackDto.getStatus());
        command.setAcknowledgedAt(ackDto.getTimestamp() != null 
                ? ackDto.getTimestamp().atOffset(ZoneOffset.UTC) 
                : OffsetDateTime.now());
        commandRepository.save(command);

        DeviceCommandAcknowledgement ack = new DeviceCommandAcknowledgement();
        ack.setCommandId(command.getCommandId());
        ack.setDeviceId(command.getDeviceId());
        ack.setAckStatus(ackDto.getStatus());
        ack.setResponseMessage(ackDto.getMessage());
        ackRepository.save(ack);

        if ("CONFIG_UPDATE".equals(command.getCommandType()) && "APPLIED".equals(ackDto.getStatus())) {
            configRepository.findByOuterDeviceId(command.getDeviceId()).ifPresent(config -> {
                if (ackDto.getConfigurationVersion() != null && ackDto.getConfigurationVersion().equals(config.getConfigurationVersion())) {
                    config.setConfigurationStatus("APPLIED");
                    config.setLastSyncedAt(OffsetDateTime.now());
                    configRepository.save(config);
                    System.out.println("DeviceConfiguration status updated to APPLIED for deviceId: " + command.getDeviceId());
                } else {
                    System.out.println("ACK configuration version mismatch. Expected " + config.getConfigurationVersion() + " but got " + ackDto.getConfigurationVersion());
                }
            });
        }
    }
}
