package com.diasmart.springapi.deviceconfig.service.impl;

import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.deviceconfig.dto.CreateDeviceConfigurationRequestDTO;
import com.diasmart.springapi.deviceconfig.dto.DeviceConfigurationResponseDTO;
import com.diasmart.springapi.deviceconfig.dto.UpdateDeviceConfigurationRequestDTO;
import com.diasmart.springapi.deviceconfig.entity.DeviceCommand;
import com.diasmart.springapi.deviceconfig.entity.DeviceConfiguration;
import com.diasmart.springapi.deviceconfig.mapper.DeviceConfigurationMapper;
import com.diasmart.springapi.deviceconfig.repository.DeviceCommandRepository;
import com.diasmart.springapi.deviceconfig.repository.DeviceConfigurationRepository;
import com.diasmart.springapi.deviceconfig.service.DeviceConfigurationService;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.mqtt.service.MqttService;
import com.diasmart.springapi.relationships.service.PatientAccessService;
import com.diasmart.springapi.shared.exceptions.ResourceNotFoundException;
import com.diasmart.springapi.shared.security.EncryptionService;
import com.diasmart.springapi.shared.security.EncryptionService.EncryptedPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeviceConfigurationServiceImpl implements DeviceConfigurationService {

    private static final String DEVICE_TYPE_OUTER = "OUTER_GATEWAY";
    private static final String DEVICE_TYPE_INNER = "INNER_UNIT";
    private static final String DEVICE_TYPE_PEN = "DOSE_CAP";
    private static final String DEVICE_TYPE_GLUCOMETER = "GLUCOMETER";
    private static final String COMMAND_TYPE_WIFI_CONFIGURATION = "WIFI_CONFIGURATION";
    private static final int MQTT_QOS_ONE = 1;

    private final DeviceConfigurationRepository configRepository;
    private final DeviceCommandRepository commandRepository;
    private final DeviceRepository deviceRepository;
    private final PatientAccessService patientAccessService;
    private final EncryptionService encryptionService;
    private final MqttService mqttService;
    private final ObjectMapper objectMapper;

    public DeviceConfigurationServiceImpl(
            DeviceConfigurationRepository configRepository,
            DeviceCommandRepository commandRepository,
            DeviceRepository deviceRepository,
            PatientAccessService patientAccessService,
            EncryptionService encryptionService,
            MqttService mqttService,
            ObjectMapper objectMapper) {
        this.configRepository = configRepository;
        this.commandRepository = commandRepository;
        this.deviceRepository = deviceRepository;
        this.patientAccessService = patientAccessService;
        this.encryptionService = encryptionService;
        this.mqttService = mqttService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public DeviceConfigurationResponseDTO createConfiguration(CreateDeviceConfigurationRequestDTO dto) {
        Device device = getValidatedOuterDevice(dto.getOuterDeviceId());

        if (configRepository.existsByOuterDeviceId(device.getDeviceId())) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFIG_ALREADY_EXISTS", "Configuration already exists for this device");
        }

        validateMappedDevice(dto.getInnerDeviceId(), DEVICE_TYPE_INNER, device.getPatientId());
        validateMappedDevice(dto.getPenDeviceId(), DEVICE_TYPE_PEN, device.getPatientId());
        validateMappedDevice(dto.getGlucometerDeviceId(), DEVICE_TYPE_GLUCOMETER, device.getPatientId());

        DeviceConfiguration config = new DeviceConfiguration();
        config.setOuterDeviceId(device.getDeviceId());
        config.setPatientId(device.getPatientId());
        config.setWifiSsid(dto.getWifiSsid());
        setEncryptedWifiPassword(config, dto.getWifiPassword());
        config.setInnerDeviceId(dto.getInnerDeviceId());
        config.setPenDeviceId(dto.getPenDeviceId());
        config.setGlucometerDeviceId(dto.getGlucometerDeviceId());
        config.setConfigurationVersion(1);
        config.setConfigurationStatus("PENDING");
        config.setOuterUnitStatus("PENDING");
        config.setInnerUnitStatus("NOT_CONFIGURED");

        config = configRepository.save(config);

        publishConfigCommand(device, config, dto.getWifiPassword());

        return DeviceConfigurationMapper.toResponseDTO(config);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceConfigurationResponseDTO> getConfigurations() {
        List<Long> patientIds = patientAccessService.getViewablePatientIdsForCurrentUser();

        if (patientIds.isEmpty()) {
            return List.of();
        }

        return configRepository.findByPatientIdInOrderByUpdatedAtDesc(patientIds)
                .stream()
                .map(DeviceConfigurationMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceConfigurationResponseDTO getConfiguration(Long outerDeviceId) {
        getValidatedOuterDevice(outerDeviceId);

        DeviceConfiguration config = configRepository.findByOuterDeviceId(outerDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration not found for this device"));

        return DeviceConfigurationMapper.toResponseDTO(config);
    }

    @Override
    @Transactional
    public DeviceConfigurationResponseDTO updateConfiguration(Long outerDeviceId, UpdateDeviceConfigurationRequestDTO dto) {
        Device device = getValidatedOuterDevice(outerDeviceId);

        DeviceConfiguration config = configRepository.findByOuterDeviceId(outerDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration not found for this device"));

        String plainTextPassword = null;
        boolean publishRequired = false;

        if (dto.getWifiSsid() != null && !dto.getWifiSsid().isBlank()) {
            config.setWifiSsid(dto.getWifiSsid());
            publishRequired = true;
        }

        if (dto.getWifiPassword() != null && !dto.getWifiPassword().isBlank()) {
            plainTextPassword = dto.getWifiPassword();
            setEncryptedWifiPassword(config, plainTextPassword);
            publishRequired = true;
        }

        if (dto.getInnerDeviceId() != null) {
            validateMappedDevice(dto.getInnerDeviceId(), DEVICE_TYPE_INNER, device.getPatientId());
            config.setInnerDeviceId(dto.getInnerDeviceId());
            publishRequired = true;
        }

        if (dto.getPenDeviceId() != null) {
            validateMappedDevice(dto.getPenDeviceId(), DEVICE_TYPE_PEN, device.getPatientId());
            config.setPenDeviceId(dto.getPenDeviceId());
            publishRequired = true;
        }

        if (dto.getGlucometerDeviceId() != null) {
            validateMappedDevice(dto.getGlucometerDeviceId(), DEVICE_TYPE_GLUCOMETER, device.getPatientId());
            config.setGlucometerDeviceId(dto.getGlucometerDeviceId());
            publishRequired = true;
        }

        if (publishRequired) {
            if (plainTextPassword == null) {
                plainTextPassword = decryptWifiPassword(config);
            }

            config.setConfigurationStatus("PENDING");
            config.setOuterUnitStatus("PENDING");
            config.setConfigurationVersion(config.getConfigurationVersion() + 1);
            config = configRepository.save(config);
            publishConfigCommand(device, config, plainTextPassword);
        }

        return DeviceConfigurationMapper.toResponseDTO(config);
    }

    @Override
    @Transactional
    public DeviceConfigurationResponseDTO sendConfiguration(Long outerDeviceId) {
        Device device = getValidatedOuterDevice(outerDeviceId);

        DeviceConfiguration config = configRepository.findByOuterDeviceId(outerDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration not found for this device"));

        config.setConfigurationStatus("PENDING");
        config.setOuterUnitStatus("PENDING");
        config = configRepository.save(config);

        publishConfigCommand(device, config, decryptWifiPassword(config));

        return DeviceConfigurationMapper.toResponseDTO(config);
    }

    @Transactional
    public void sendConfigurationForDeviceSync(Device device) {
        if (device == null || device.getDeviceId() == null) {
            return;
        }

        configRepository.findByOuterDeviceId(device.getDeviceId()).ifPresent(config -> {
            config.setConfigurationStatus("PENDING");
            config.setOuterUnitStatus("PENDING");
            DeviceConfiguration savedConfig = configRepository.save(config);
            publishConfigCommand(device, savedConfig, decryptWifiPassword(savedConfig));
        });
    }

    private Device getValidatedOuterDevice(Long outerDeviceId) {
        Device device = deviceRepository.findById(outerDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Outer device not found"));

        if (!DEVICE_TYPE_OUTER.equals(device.getDeviceType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DEVICE_TYPE", "Device is not an OUTER_GATEWAY");
        }

        if (!Boolean.TRUE.equals(device.getActive())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DEVICE_INACTIVE", "Device is not active");
        }

        if (device.getPatientId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DEVICE_NOT_ASSIGNED", "Device is not assigned to a patient");
        }

        patientAccessService.requireCanViewPatient(device.getPatientId());

        return device;
    }

    private void validateMappedDevice(Long deviceId, String expectedType, Long patientId) {
        if (deviceId == null) {
            return;
        }

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Mapped device not found: " + deviceId));

        if (!expectedType.equals(device.getDeviceType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DEVICE_MAPPING", "Mapped device " + deviceId + " is not a " + expectedType);
        }

        if (!Boolean.TRUE.equals(device.getActive())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MAPPED_DEVICE_INACTIVE", "Mapped device " + deviceId + " is inactive");
        }

        if (device.getPatientId() != null && !device.getPatientId().equals(patientId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "DEVICE_MAPPING_FORBIDDEN", "Mapped device belongs to another patient");
        }
    }

    private void setEncryptedWifiPassword(DeviceConfiguration config, String plainTextPassword) {
        EncryptedPayload encrypted = encryptionService.encryptStructured(plainTextPassword);
        config.setWifiPasswordCiphertext(encrypted.getCiphertext());
        config.setWifiPasswordNonce(encrypted.getNonce());
        config.setWifiPasswordTag(encrypted.getAuthTag());
        config.setWifiPassword(toLegacyEncryptedBundle(encrypted));
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

    private String toLegacyEncryptedBundle(EncryptedPayload encrypted) {
        byte[] nonce = Base64.getDecoder().decode(encrypted.getNonce());
        byte[] ciphertext = Base64.getDecoder().decode(encrypted.getCiphertext());
        byte[] authTag = Base64.getDecoder().decode(encrypted.getAuthTag());
        byte[] bundled = new byte[nonce.length + ciphertext.length + authTag.length];

        System.arraycopy(nonce, 0, bundled, 0, nonce.length);
        System.arraycopy(ciphertext, 0, bundled, nonce.length, ciphertext.length);
        System.arraycopy(authTag, 0, bundled, nonce.length + ciphertext.length, authTag.length);

        return Base64.getEncoder().encodeToString(bundled);
    }

    private void publishConfigCommand(Device device, DeviceConfiguration config, String plainTextPassword) {
        DeviceCommand command = new DeviceCommand();
        command.setDeviceId(device.getDeviceId());
        command.setPatientId(device.getPatientId());
        command.setCommandType(COMMAND_TYPE_WIFI_CONFIGURATION);
        command.setCommandStatus("PENDING");
        command.setPayload("{}");
        command = commandRepository.save(command);

        command.setCommandUid("CMD-" + command.getCommandId());

        try {
            String payloadJson = buildWifiCommandPayload(command, device, config, plainTextPassword);
            command.setPayload(payloadJson);
            command = commandRepository.save(command);

            publishWithRetry(command, config, "diasmart/devices/" + device.getDeviceUid() + "/commands", payloadJson, false);
        } catch (JsonProcessingException e) {
            command.setCommandStatus("FAILED");
            command.setLastError("Failed to serialize command payload");
            commandRepository.save(command);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PAYLOAD_SERIALIZATION_ERROR", "Failed to serialize command payload");
        }
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
        RuntimeException lastException = null;

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
                lastException = ex;
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
