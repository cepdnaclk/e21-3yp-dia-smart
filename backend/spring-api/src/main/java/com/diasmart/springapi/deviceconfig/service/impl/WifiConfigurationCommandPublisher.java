package com.diasmart.springapi.deviceconfig.service.impl;

import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.deviceconfig.entity.DeviceCommand;
import com.diasmart.springapi.deviceconfig.entity.DeviceConfiguration;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.mqtt.service.MqttService;
import com.diasmart.springapi.shared.security.EncryptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class WifiConfigurationCommandPublisher {

    private static final String COMMAND_TYPE_WIFI_CONFIGURATION = "WIFI_CONFIGURATION";
    private static final int MQTT_QOS_ONE = 1;

    private final WifiCommandStateService stateService;
    private final EncryptionService encryptionService;
    private final MqttService mqttService;
    private final ObjectMapper objectMapper;

    public WifiConfigurationCommandPublisher(
            WifiCommandStateService stateService,
            EncryptionService encryptionService,
            MqttService mqttService,
            ObjectMapper objectMapper) {
        this.stateService = stateService;
        this.encryptionService = encryptionService;
        this.mqttService = mqttService;
        this.objectMapper = objectMapper;
    }

    public boolean publishWifiCommand(Long commandId) {
        if (!stateService.claimForPublish(commandId)) {
            return false;
        }

        WifiCommandPublishContext context = null;
        Long configurationId = null;

        String plainTextPassword = null;
        String payloadJson = null;
        try {
            try {
                context = stateService.loadContext(commandId);
                configurationId = context.configuration().getConfigurationId();
            } catch (ApiException ex) {
                stateService.markNonRetryableFailure(commandId, configurationId, ex.getErrorCode());
                throw ex;
            }

            try {
                plainTextPassword = decryptWifiPassword(context.configuration());
                payloadJson = buildWifiCommandPayload(context.command(), context.outerDevice(), context.configuration(), plainTextPassword);
            } catch (JsonProcessingException e) {
                stateService.markNonRetryableFailure(commandId, configurationId, "PAYLOAD_SERIALIZATION_ERROR");
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PAYLOAD_SERIALIZATION_ERROR", "Failed to serialize command payload");
            } catch (RuntimeException ex) {
                stateService.markNonRetryableFailure(commandId, configurationId, "WIFI_PASSWORD_DECRYPT_FAILED");
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "WIFI_PASSWORD_DECRYPT_FAILED", "Failed to prepare WiFi command credentials");
            }

            try {
                mqttService.publish(
                        "diasmart/devices/" + context.outerDevice().getDeviceUid() + "/commands",
                        payloadJson,
                        MQTT_QOS_ONE,
                        false
                );
                stateService.markPublished(commandId, configurationId);
                return true;
            } catch (RuntimeException ex) {
                stateService.markRetryableFailure(commandId, configurationId, "MQTT_PUBLISH_FAILED");
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "MQTT_PUBLISH_ERROR", "Failed to publish MQTT command");
            }
        } finally {
            plainTextPassword = null;
            payloadJson = null;
            context = null;
        }
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
        payload.put("innerDeviceId", stateService.findDeviceUid(config.getInnerDeviceId()));
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
