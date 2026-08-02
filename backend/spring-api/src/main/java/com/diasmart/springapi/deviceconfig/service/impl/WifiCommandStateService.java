package com.diasmart.springapi.deviceconfig.service.impl;

import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.deviceconfig.entity.DeviceCommand;
import com.diasmart.springapi.deviceconfig.entity.DeviceConfiguration;
import com.diasmart.springapi.deviceconfig.repository.DeviceCommandRepository;
import com.diasmart.springapi.deviceconfig.repository.DeviceConfigurationRepository;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class WifiCommandStateService {

    private static final String DEVICE_TYPE_OUTER = "OUTER_GATEWAY";
    private static final String COMMAND_TYPE_WIFI_CONFIGURATION = "WIFI_CONFIGURATION";

    private final DeviceCommandRepository commandRepository;
    private final DeviceConfigurationRepository configRepository;
    private final DeviceRepository deviceRepository;
    private final WifiCommandPublishProperties properties;

    public WifiCommandStateService(
            DeviceCommandRepository commandRepository,
            DeviceConfigurationRepository configRepository,
            DeviceRepository deviceRepository,
            WifiCommandPublishProperties properties) {
        this.commandRepository = commandRepository;
        this.configRepository = configRepository;
        this.deviceRepository = deviceRepository;
        this.properties = properties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimForPublish(Long commandId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime staleSentBefore = now.minusSeconds(properties.getSentStaleAfterSeconds());

        return commandRepository.claimRecoverableWifiCommand(
                commandId,
                now,
                staleSentBefore,
                properties.getMaxRetries()
        ) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public WifiCommandPublishContext loadContext(Long commandId) {
        DeviceCommand command = commandRepository.findById(commandId)
                .orElseThrow(() -> api(HttpStatus.NOT_FOUND, "COMMAND_NOT_FOUND", "WiFi command not found"));

        if (!COMMAND_TYPE_WIFI_CONFIGURATION.equals(command.getCommandType())) {
            throw api(HttpStatus.BAD_REQUEST, "INVALID_COMMAND_TYPE", "Command is not a WiFi configuration command");
        }

        if (!"SENT".equals(command.getCommandStatus())) {
            throw api(HttpStatus.CONFLICT, "COMMAND_NOT_CLAIMED", "WiFi command was not claimed for publishing");
        }

        Long configurationId = command.getDeviceConfigurationId();
        if (configurationId == null || command.getConfigurationVersion() == null) {
            throw api(HttpStatus.CONFLICT, "CONFIGURATION_REFERENCE_MISSING", "WiFi command is missing configuration metadata");
        }

        DeviceConfiguration config = configRepository.findByConfigurationId(configurationId)
                .orElseThrow(() -> api(HttpStatus.NOT_FOUND, "CONFIGURATION_NOT_FOUND", "Configuration not found for WiFi command"));

        if (!command.getConfigurationVersion().equals(config.getConfigurationVersion())) {
            throw api(HttpStatus.CONFLICT, "CONFIGURATION_VERSION_MISMATCH", "WiFi command configuration version is stale");
        }

        if (command.getDeviceId() == null || !command.getDeviceId().equals(config.getOuterDeviceId())) {
            throw api(HttpStatus.CONFLICT, "CONFIGURATION_DEVICE_MISMATCH", "WiFi command device does not match configuration");
        }

        if (command.getPatientId() == null) {
            throw api(HttpStatus.CONFLICT, "COMMAND_DEVICE_REFERENCE_MISSING", "WiFi command is missing device metadata");
        }

        Device device = deviceRepository.findById(command.getDeviceId())
                .orElseThrow(() -> api(HttpStatus.NOT_FOUND, "DEVICE_NOT_FOUND", "Outer device not found for WiFi command"));

        if (!DEVICE_TYPE_OUTER.equals(device.getDeviceType())) {
            throw api(HttpStatus.BAD_REQUEST, "INVALID_DEVICE_TYPE", "Command device is not an OUTER_GATEWAY");
        }

        if (!Boolean.TRUE.equals(device.getActive())) {
            throw api(HttpStatus.BAD_REQUEST, "DEVICE_INACTIVE", "Outer device is inactive");
        }

        if (device.getPatientId() == null) {
            throw api(HttpStatus.BAD_REQUEST, "DEVICE_NOT_ASSIGNED", "Outer device is not assigned to a patient");
        }

        if (!device.getPatientId().equals(command.getPatientId()) || !device.getPatientId().equals(config.getPatientId())) {
            throw api(HttpStatus.CONFLICT, "DEVICE_PATIENT_MISMATCH", "WiFi command patient does not match outer device");
        }

        return new WifiCommandPublishContext(command, config, device);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public String findDeviceUid(Long deviceId) {
        if (deviceId == null) {
            return null;
        }

        return deviceRepository.findById(deviceId)
                .map(Device::getDeviceUid)
                .orElse(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(Long commandId, Long configurationId) {
        OffsetDateTime now = OffsetDateTime.now();

        commandRepository.findById(commandId).ifPresent(command -> {
            command.setCommandStatus("PUBLISHED");
            command.setPublishedAt(now);
            command.setNextRetryAt(null);
            command.setLastError(null);
            commandRepository.save(command);
        });

        if (configurationId != null) {
            configRepository.findByConfigurationId(configurationId).ifPresent(config -> {
                config.setConfigurationStatus("PUBLISHED");
                config.setOuterUnitStatus("PUBLISHED");
                configRepository.save(config);
            });
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRetryableFailure(Long commandId, Long configurationId, String errorCode) {
        OffsetDateTime now = OffsetDateTime.now();
        commandRepository.findById(commandId)
                .ifPresent(command -> {
                    int nextAttemptCount = safeRetryCount(command) + 1;
                    command.setRetryCount(nextAttemptCount);
                    command.setCommandStatus("FAILED");
                    command.setLastAttemptAt(now);
                    command.setLastError(nextAttemptCount >= properties.getMaxRetries() ? "RETRY_LIMIT_REACHED" : errorCode);
                    command.setNextRetryAt(nextAttemptCount >= properties.getMaxRetries()
                            ? null
                            : now.plusSeconds(properties.getRetryDelaySeconds()));
                    commandRepository.save(command);
                });

        if (configurationId != null) {
            configRepository.findByConfigurationId(configurationId).ifPresent(config -> {
                config.setConfigurationStatus("FAILED");
                config.setOuterUnitStatus("FAILED");
                configRepository.save(config);
            });
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markNonRetryableFailure(Long commandId, Long configurationId, String errorCode) {
        OffsetDateTime now = OffsetDateTime.now();

        commandRepository.findById(commandId).ifPresent(command -> {
            command.setCommandStatus("FAILED");
            command.setRetryCount(properties.getMaxRetries());
            command.setLastAttemptAt(now);
            command.setNextRetryAt(null);
            command.setLastError(errorCode);
            commandRepository.save(command);
        });

        if (configurationId != null) {
            configRepository.findByConfigurationId(configurationId).ifPresent(config -> {
                config.setConfigurationStatus("FAILED");
                config.setOuterUnitStatus("FAILED");
                configRepository.save(config);
            });
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void expireSupersededWifiCommands(Long configurationId, Integer configurationVersion) {
        if (configurationId == null || configurationVersion == null) {
            return;
        }

        commandRepository.expireSupersededWifiCommands(configurationId, configurationVersion);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<Long> findRecoverableWifiCommandIds() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime staleSentBefore = now.minusSeconds(properties.getSentStaleAfterSeconds());

        return commandRepository.findRecoverableWifiCommandIds(
                now,
                staleSentBefore,
                properties.getMaxRetries(),
                PageRequest.of(0, properties.getRecoveryBatchSize())
        );
    }

    private int safeRetryCount(DeviceCommand command) {
        return command.getRetryCount() == null ? 0 : command.getRetryCount();
    }

    private ApiException api(HttpStatus status, String errorCode, String message) {
        return new ApiException(status, errorCode, message);
    }
}
