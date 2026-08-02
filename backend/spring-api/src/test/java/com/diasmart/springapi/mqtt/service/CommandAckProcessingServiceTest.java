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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandAckProcessingServiceTest {

    @Mock
    private DeviceCommandRepository commandRepository;

    @Mock
    private DeviceCommandAcknowledgementRepository acknowledgementRepository;

    @Mock
    private DeviceConfigurationRepository configurationRepository;

    @Mock
    private DeviceRepository deviceRepository;

    private CommandAckProcessingService service;

    @BeforeEach
    void setUp() {
        service = new CommandAckProcessingService(
                commandRepository,
                acknowledgementRepository,
                configurationRepository,
                deviceRepository
        );
    }

    @Test
    void validAckShouldUpdateCommandAndConfiguration() {
        DeviceCommand command = wifiCommand();
        DeviceConfiguration config = configuration();

        when(commandRepository.findByCommandUid("CMD-25")).thenReturn(Optional.of(command));
        when(acknowledgementRepository.existsByAckDeduplicationKey("FW|CMD-25|ACK-1")).thenReturn(false);
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(outerDevice("OUTER-001")));
        when(configurationRepository.findByConfigurationId(11L)).thenReturn(Optional.of(config));

        service.processAck(ack("VALIDATED", 3, "ACK-1", "OUTER-001"), "OUTER-001");

        ArgumentCaptor<DeviceCommandAcknowledgement> ackCaptor = ArgumentCaptor.forClass(DeviceCommandAcknowledgement.class);
        verify(acknowledgementRepository).save(ackCaptor.capture());
        assertEquals("VALIDATED", ackCaptor.getValue().getAckStatus());
        assertEquals("ACCEPTED", ackCaptor.getValue().getProcessingResult());
        assertEquals(3, ackCaptor.getValue().getConfigurationVersion());
        assertEquals("OUTER-001", ackCaptor.getValue().getReportingOuterDeviceUid());

        assertEquals("VALIDATED", command.getCommandStatus());
        assertNotNull(command.getAcknowledgedAt());
        assertEquals("VALIDATED", config.getConfigurationStatus());
        assertEquals("VALIDATED", config.getOuterUnitStatus());
        verify(commandRepository).save(command);
        verify(configurationRepository).save(config);
    }

    @Test
    void duplicateAckShouldNotPersistOrRepeatSideEffects() {
        DeviceCommand command = wifiCommand();

        when(commandRepository.findByCommandUid("CMD-25")).thenReturn(Optional.of(command));
        when(acknowledgementRepository.existsByAckDeduplicationKey("FW|CMD-25|ACK-1")).thenReturn(true);

        service.processAck(ack("VALIDATED", 3, "ACK-1", "OUTER-001"), "OUTER-001");

        verify(acknowledgementRepository, never()).save(any());
        verify(commandRepository, never()).save(any());
        verify(configurationRepository, never()).save(any());
    }

    @Test
    void wrongTopicOuterShouldPersistRejectedAckWithoutUpdatingState() {
        DeviceCommand command = wifiCommand();

        when(commandRepository.findByCommandUid("CMD-25")).thenReturn(Optional.of(command));
        when(acknowledgementRepository.existsByAckDeduplicationKey("FW|CMD-25|ACK-1")).thenReturn(false);
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(outerDevice("OUTER-001")));

        service.processAck(ack("VALIDATED", 3, "ACK-1", "OUTER-001"), "OUTER-002");

        ArgumentCaptor<DeviceCommandAcknowledgement> ackCaptor = ArgumentCaptor.forClass(DeviceCommandAcknowledgement.class);
        verify(acknowledgementRepository).save(ackCaptor.capture());
        assertEquals("REPORTING_OUTER_UID_MISMATCH", ackCaptor.getValue().getProcessingResult());
        assertEquals("OUTER-002", ackCaptor.getValue().getReportingOuterDeviceUid());
        verify(commandRepository, never()).save(any());
        verify(configurationRepository, never()).save(any());
    }

    @Test
    void staleAckVersionShouldBeRejectedWithoutUpdatingConfiguration() {
        DeviceCommand command = wifiCommand();
        DeviceConfiguration config = configuration();

        when(commandRepository.findByCommandUid("CMD-25")).thenReturn(Optional.of(command));
        when(acknowledgementRepository.existsByAckDeduplicationKey("FW|CMD-25|ACK-1")).thenReturn(false);
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(outerDevice("OUTER-001")));
        when(configurationRepository.findByConfigurationId(11L)).thenReturn(Optional.of(config));

        service.processAck(ack("VALIDATED", 2, "ACK-1", "OUTER-001"), "OUTER-001");

        ArgumentCaptor<DeviceCommandAcknowledgement> ackCaptor = ArgumentCaptor.forClass(DeviceCommandAcknowledgement.class);
        verify(acknowledgementRepository).save(ackCaptor.capture());
        assertEquals("ACK_CONFIGURATION_VERSION_MISMATCH", ackCaptor.getValue().getProcessingResult());
        verify(commandRepository, never()).save(any());
        verify(configurationRepository, never()).save(any());
    }

    private CommandAckDTO ack(String status, int version, String acknowledgementId, String outerUid) {
        CommandAckDTO ack = new CommandAckDTO();
        ack.setCommandId("CMD-25");
        ack.setAcknowledgementId(acknowledgementId);
        ack.setCommandType("WIFI_CONFIGURATION");
        ack.setStatus(status);
        ack.setOuterDeviceId(outerUid);
        ack.setConfigurationVersion(version);
        ack.setMessage("Configuration " + status);
        ack.setTimestamp(Instant.parse("2026-08-02T12:30:00Z"));
        return ack;
    }

    private DeviceCommand wifiCommand() {
        DeviceCommand command = new DeviceCommand();
        command.setCommandId(25L);
        command.setCommandUid("CMD-25");
        command.setDeviceId(1L);
        command.setPatientId(10L);
        command.setDeviceConfigurationId(11L);
        command.setConfigurationVersion(3);
        command.setCommandType("WIFI_CONFIGURATION");
        command.setCommandStatus("PUBLISHED");
        command.setPayload("{}");
        return command;
    }

    private DeviceConfiguration configuration() {
        DeviceConfiguration config = new DeviceConfiguration();
        config.setConfigurationId(11L);
        config.setOuterDeviceId(1L);
        config.setPatientId(10L);
        config.setConfigurationVersion(3);
        config.setConfigurationStatus("PUBLISHED");
        return config;
    }

    private Device outerDevice(String uid) {
        Device device = new Device();
        device.setDeviceId(1L);
        device.setDeviceUid(uid);
        device.setDeviceType("OUTER_GATEWAY");
        device.setPatientId(10L);
        device.setActive(true);
        return device;
    }
}
