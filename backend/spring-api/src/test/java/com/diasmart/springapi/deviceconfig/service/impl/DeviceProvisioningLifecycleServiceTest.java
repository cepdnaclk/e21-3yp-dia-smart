package com.diasmart.springapi.deviceconfig.service.impl;

import com.diasmart.springapi.deviceconfig.dto.DeviceConfigurationResponseDTO;
import com.diasmart.springapi.deviceconfig.entity.DeviceCommand;
import com.diasmart.springapi.deviceconfig.entity.DeviceConfiguration;
import com.diasmart.springapi.devices.entity.Device;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DeviceProvisioningLifecycleServiceTest {

    private final DeviceProvisioningLifecycleService service = new DeviceProvisioningLifecycleService();

    @Test
    void appliedOuterAndConnectedInnerShouldReturnSucceededTerminalStatus() {
        DeviceConfiguration config = configuration();
        config.setConfigurationStatus("APPLIED");
        config.setOuterUnitStatus("APPLIED");
        config.setInnerUnitStatus("CONNECTED");
        config.setMqttStatus("CONNECTED");

        DeviceCommand command = command();
        command.setCommandStatus("APPLIED");
        command.setCompletedAt(OffsetDateTime.parse("2026-08-02T12:32:00Z"));

        DeviceConfigurationResponseDTO response = service.toStatusResponse(config, outer(), command, null, null);

        assertEquals("SUCCEEDED", response.getOverallStatus());
        assertTrue(response.getTerminal());
        assertEquals("CMD-25", response.getCommandId());
        assertEquals("OUTER-001", response.getOuterDeviceUid());
    }

    @Test
    void expiredTimeoutShouldReturnTimedOutWithoutMutatingCommand() {
        DeviceConfiguration config = configuration();
        DeviceCommand command = command();
        command.setCommandStatus("APPLIED");
        command.setTimeoutAt(OffsetDateTime.now().minusMinutes(1));

        DeviceConfigurationResponseDTO response = service.toStatusResponse(config, outer(), command, null, null);

        assertEquals("TIMED_OUT", response.getOverallStatus());
        assertTrue(response.getTerminal());
        assertEquals("INNER_RESULT_TIMEOUT", response.getLastErrorCode());
        assertNull(command.getCompletedAt());
    }

    private DeviceConfiguration configuration() {
        DeviceConfiguration config = new DeviceConfiguration();
        config.setConfigurationId(11L);
        config.setOuterDeviceId(1L);
        config.setPatientId(10L);
        config.setConfigurationVersion(3);
        config.setConfigurationStatus("APPLYING");
        config.setOuterUnitStatus("APPLIED");
        config.setInnerUnitStatus("CONNECTING");
        config.setMqttStatus("CONNECTED");
        config.setRollbackStatus("NOT_REQUIRED");
        return config;
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
        command.setCommandStatus("PUBLISHED");
        command.setPayload("{}");
        return command;
    }

    private Device outer() {
        Device device = new Device();
        device.setDeviceId(1L);
        device.setDeviceUid("OUTER-001");
        return device;
    }
}
