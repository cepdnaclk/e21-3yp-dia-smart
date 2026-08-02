package com.diasmart.springapi.deviceconfig.service.impl;

import com.diasmart.springapi.deviceconfig.entity.DeviceCommand;
import com.diasmart.springapi.deviceconfig.entity.DeviceConfiguration;
import com.diasmart.springapi.deviceconfig.repository.DeviceCommandRepository;
import com.diasmart.springapi.deviceconfig.repository.DeviceConfigurationRepository;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WifiCommandStateServiceTest {

    @Mock
    private DeviceCommandRepository commandRepository;

    @Mock
    private DeviceConfigurationRepository configRepository;

    @Mock
    private DeviceRepository deviceRepository;

    private WifiCommandStateService service;

    @BeforeEach
    void setUp() {
        service = new WifiCommandStateService(
                commandRepository,
                configRepository,
                deviceRepository,
                new WifiCommandPublishProperties(),
                new DeviceProvisioningProperties()
        );
    }

    @Test
    void markProvisioningTimedOutShouldUpdateCurrentCommandAndConfiguration() {
        DeviceCommand command = command();
        DeviceConfiguration config = configuration();

        when(commandRepository.findById(25L)).thenReturn(Optional.of(command));
        when(configRepository.findByConfigurationId(11L)).thenReturn(Optional.of(config));

        service.markProvisioningTimedOut(25L);

        assertEquals("TIMED_OUT", command.getCommandStatus());
        assertEquals("PROVISIONING_TIMEOUT", command.getLastError());
        assertNotNull(command.getCompletedAt());
        assertEquals("TIMED_OUT", config.getConfigurationStatus());
        assertEquals("INNER_RESULT_TIMEOUT", config.getProvisioningFailureCode());
        assertNotNull(config.getProvisioningCompletedAt());
        verify(commandRepository).save(command);
        verify(configRepository).save(config);
    }

    @Test
    void markProvisioningTimedOutShouldIgnoreCompletedCommands() {
        DeviceCommand command = command();
        command.setCompletedAt(OffsetDateTime.now());
        when(commandRepository.findById(25L)).thenReturn(Optional.of(command));

        service.markProvisioningTimedOut(25L);

        verify(commandRepository, never()).save(any(DeviceCommand.class));
        verify(configRepository, never()).save(any(DeviceConfiguration.class));
    }

    private DeviceCommand command() {
        DeviceCommand command = new DeviceCommand();
        command.setCommandId(25L);
        command.setCommandUid("CMD-25");
        command.setDeviceId(1L);
        command.setPatientId(10L);
        command.setDeviceConfigurationId(11L);
        command.setConfigurationVersion(3);
        command.setCommandType("WIFI_CONFIGURATION");
        command.setCommandStatus("APPLIED");
        command.setPayload("{}");
        command.setPublishedAt(OffsetDateTime.now().minusMinutes(10));
        command.setTimeoutAt(OffsetDateTime.now().minusMinutes(5));
        return command;
    }

    private DeviceConfiguration configuration() {
        DeviceConfiguration config = new DeviceConfiguration();
        config.setConfigurationId(11L);
        config.setOuterDeviceId(1L);
        config.setPatientId(10L);
        config.setConfigurationVersion(3);
        config.setConfigurationStatus("APPLYING");
        return config;
    }
}
