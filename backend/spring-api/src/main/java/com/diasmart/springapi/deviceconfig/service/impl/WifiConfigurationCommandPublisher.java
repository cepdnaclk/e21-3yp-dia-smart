package com.diasmart.springapi.deviceconfig.service.impl;

import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.deviceconfig.entity.DeviceCommand;
import com.diasmart.springapi.deviceconfig.entity.DeviceConfiguration;
import com.diasmart.springapi.deviceconfig.repository.DeviceCommandRepository;
import com.diasmart.springapi.deviceconfig.repository.DeviceConfigurationRepository;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.mqtt.service.MqttService;
import com.diasmart.springapi.shared.security.EncryptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class WifiConfigurationCommandPublisher {

    private static final String DEVICE_TYPE_OUTER = "OUTER_GATEWAY";
    private static final String COMMAND_TYPE_WIFI_CONFIGURATION = "WIFI_CONFIGURATION";
    private static final int MQTT_QOS_ONE = 1;

    private final DeviceCommandRepository commandRepository;
    private final DeviceConfigurationRepository configRepository;
    private final DeviceRepository deviceRepository;
    private final EncryptionService encryptionService;
    private final MqttService mqttService;
    private final ObjectMapper objectMapper;

    public WifiConfigurationCommandPublisher(
            DeviceCommandRepository commandRepository,
            DeviceConfigurationRepository configRepository,
            DeviceRepository deviceRepository,
            EncryptionService encryptionService,
            MqttService mqttService,
            ObjectMapper objectMapper) {
        this.commandRepository = commandRepository;
        this.configRepository = configRepository;
        this.deviceRepository = deviceRepository;
        this.encryptionService = encryptionService;
        this.mqttService = mqttService;
        this.objectMapper = objectMapper;
    }

    public void publishWifiCommand(Long commandId) {
        DeviceCommand command = commandRepository.findById(commandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMMAND_NOT_FOUND", "WiFi command not found"));

        if (!COMMAND_TYPE_WIFI_CONFIGURATION.equals(command.getCommandType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_COMMAND_TYPE", "Command is not a WiFi configuration command");
        }

        DeviceConfiguration config = loadConfiguration(command);
        Device device = loadOuterDevice(command, config);

        String plainTextPassword = null;
        String payloadJson = null;
        try {
            plainTextPassword = decryptWifiPassword(config);
            payloadJson = buildWifiCommandPayload(command, device, config, plainTextPassword);
            publishWithRetry(command, config, "diasmart/devices/" + device.getDeviceUid() + "/commands", payloadJson, false);
        } catch (JsonProcessingException e) {
            markCommandFailed(command, "PAYLOAD_SERIALIZATION_ERROR");
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PAYLOAD_SERIALIZATION_ERROR", "Failed to serialize command payload");
        } finally {
            plainTextPassword = null;
            payloadJson = null;
        }
    }

    private DeviceConfiguration loadConfiguration(DeviceCommand command) {
        Long configurationId = command.getDeviceConfigurationId();
        if (configurationId == null || command.getConfigurationVersion() == null) {
            markCommandFailed(command, "CONFIGURATION_REFERENCE_MISSING");
            throw new ApiException(HttpStatus.CONFLICT, "CONFIGURATION_REFERENCE_MISSING", "WiFi command is missing configuration metadata");
        }

        DeviceConfiguration config = configRepository.findByConfigurationId(configurationId)
                .orElse(null);

        if (config == null) {
            markCommandFailed(command, "CONFIGURATION_NOT_FOUND");
            throw new ApiException(HttpStatus.NOT_FOUND, "CONFIGURATION_NOT_FOUND", "Configuration not found for WiFi command");
        }

        if (!configurationId.equals(config.getConfigurationId())) {
            markCommandFailed(command, "CONFIGURATION_REFERENCE_MISMATCH");
            throw new ApiException(HttpStatus.CONFLICT, "CONFIGURATION_REFERENCE_MISMATCH", "WiFi command configuration reference is invalid");
        }

        if (!command.getConfigurationVersion().equals(config.getConfigurationVersion())) {
            markCommandFailed(command, "CONFIGURATION_VERSION_MISMATCH");
            throw new ApiException(HttpStatus.CONFLICT, "CONFIGURATION_VERSION_MISMATCH", "WiFi command configuration version is stale");
        }

        if (command.getDeviceId() == null || !command.getDeviceId().equals(config.getOuterDeviceId())) {
            markCommandFailed(command, "CONFIGURATION_DEVICE_MISMATCH");
            throw new ApiException(HttpStatus.CONFLICT, "CONFIGURATION_DEVICE_MISMATCH", "WiFi command device does not match configuration");
        }

        return config;
    }

    private Device loadOuterDevice(DeviceCommand command, DeviceConfiguration config) {
        if (command.getDeviceId() == null || command.getPatientId() == null) {
            markCommandFailed(command, "COMMAND_DEVICE_REFERENCE_MISSING");
            throw new ApiException(HttpStatus.CONFLICT, "COMMAND_DEVICE_REFERENCE_MISSING", "WiFi command is missing device metadata");
        }

        Device device = deviceRepository.findById(command.getDeviceId())
                .orElse(null);

        if (device == null) {
            markCommandFailed(command, "DEVICE_NOT_FOUND");
            throw new ApiException(HttpStatus.NOT_FOUND, "DEVICE_NOT_FOUND", "Outer device not found for WiFi command");
        }

        if (!DEVICE_TYPE_OUTER.equals(device.getDeviceType())) {
            markCommandFailed(command, "INVALID_DEVICE_TYPE");
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DEVICE_TYPE", "Command device is not an OUTER_GATEWAY");
        }

        if (!Boolean.TRUE.equals(device.getActive())) {
            markCommandFailed(command, "DEVICE_INACTIVE");
            throw new ApiException(HttpStatus.BAD_REQUEST, "DEVICE_INACTIVE", "Outer device is inactive");
        }

        if (device.getPatientId() == null) {
            markCommandFailed(command, "DEVICE_NOT_ASSIGNED");
            throw new ApiException(HttpStatus.BAD_REQUEST, "DEVICE_NOT_ASSIGNED", "Outer device is not assigned to a patient");
        }

        if (!device.getPatientId().equals(command.getPatientId()) || !device.getPatientId().equals(config.getPatientId())) {
            markCommandFailed(command, "DEVICE_PATIENT_MISMATCH");
            throw new ApiException(HttpStatus.CONFLICT, "DEVICE_PATIENT_MISMATCH", "WiFi command patient does not match outer device");
        }

        return device;
    }

    private String decryptWifiPassword(DeviceConfiguration config) {
        if (hasText(config.getWifiPasswordCiphertext())
                && hasText(config.getWifiPasswordNonce())
                && hasText(config.getWifiPasswordTag())) {
            return encryptionService.decryptStructured(
                    config.getWifiPasswordCiphertext(),
                    config.getWifiPasswordNonce(),
                    config.getWifiPasswordTag()
            );
        }

        return encryptionService.decrypt(config.getWifiPassword());
    }

    private String buildWifiCommandPayload(
            DeviceCommand command,
            Device device,
            DeviceConfiguration config,
            String plainTextPassword
    ) throws JsonProcessingException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("wifiSsid", config.getWifiSsid());
        payload.put("wifiPassword", plainTextPassword);
        payload.put("innerDeviceId", resolveDeviceUid(config.getInnerDeviceId()));
        payload.put("innerDeviceNumericId", config.getInnerDeviceId());
        payload.put("configurationVersion", config.getConfigurationVersion());

        Map<String, Object> commandEnvelope = new HashMap<>();
        commandEnvelope.put("commandId", command.getCommandUid());
        commandEnvelope.put("commandType", COMMAND_TYPE_WIFI_CONFIGURATION);
        commandEnvelope.put("createdAt", Instant.now().toString());
        commandEnvelope.put("outerDeviceId", device.getDeviceUid());
        commandEnvelope.put("payload", payload);

        return objectMapper.writeValueAsString(commandEnvelope);
    }

    private void publishWithRetry(
            DeviceCommand command,
            DeviceConfiguration config,
            String topic,
            String payloadJson,
            boolean retained
    ) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                mqttService.publish(topic, payloadJson, MQTT_QOS_ONE, retained);
                command.setCommandStatus("PUBLISHED");
                command.setPublishedAt(OffsetDateTime.now());
                command.setRetryCount(attempt - 1);
                command.setLastError(null);
                commandRepository.save(command);

                config.setConfigurationStatus("PUBLISHED");
                config.setOuterUnitStatus("PUBLISHED");
                configRepository.save(config);
                return;
            } catch (RuntimeException ex) {
                command.setRetryCount(attempt);
                command.setLastError(ex.getMessage());
                commandRepository.save(command);
            }
        }

        command.setCommandStatus("FAILED");
        commandRepository.save(command);

        config.setConfigurationStatus("FAILED");
        config.setOuterUnitStatus("FAILED");
        configRepository.save(config);

        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "MQTT_PUBLISH_ERROR", "Failed to publish MQTT command");
    }

    private void markCommandFailed(DeviceCommand command, String errorCode) {
        command.setCommandStatus("FAILED");
        command.setLastError(errorCode);
        commandRepository.save(command);
    }

    private String resolveDeviceUid(Long deviceId) {
        if (deviceId == null) {
            return null;
        }

        return deviceRepository.findById(deviceId)
                .map(Device::getDeviceUid)
                .orElse(null);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
