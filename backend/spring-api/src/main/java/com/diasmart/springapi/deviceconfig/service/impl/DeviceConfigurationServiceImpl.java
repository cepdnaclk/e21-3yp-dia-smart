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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeviceConfigurationServiceImpl implements DeviceConfigurationService {

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

    private Device getValidatedDevice(Long outerDeviceId) {
        Device device = deviceRepository.findById(outerDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Outer device not found"));

        if (!"OUTER_GATEWAY".equals(device.getDeviceType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Device is not an OUTER_GATEWAY", "INVALID_DEVICE_TYPE");
        }

        if (device.getPatientId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Device is not assigned to a patient", "DEVICE_NOT_ASSIGNED");
        }

        patientAccessService.requireCanViewPatient(device.getPatientId());

        return device;
    }

    @Override
    @Transactional
    public DeviceConfigurationResponseDTO createConfiguration(CreateDeviceConfigurationRequestDTO dto) {
        Device device = getValidatedDevice(dto.getOuterDeviceId());

        if (configRepository.existsByOuterDeviceId(device.getDeviceId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Configuration already exists for this device", "CONFIG_ALREADY_EXISTS");
        }

        DeviceConfiguration config = new DeviceConfiguration();
        config.setOuterDeviceId(device.getDeviceId());
        config.setPatientId(device.getPatientId());
        config.setWifiSsid(dto.getWifiSsid());
        config.setWifiPassword(encryptionService.encrypt(dto.getWifiPassword()));
        config.setInnerDeviceId(dto.getInnerDeviceId());
        config.setPenDeviceId(dto.getPenDeviceId());
        config.setGlucometerDeviceId(dto.getGlucometerDeviceId());
        config.setConfigurationVersion(1);
        config.setConfigurationStatus("PENDING");

        config = configRepository.save(config);

        publishConfigCommand(device, config, dto.getWifiPassword());

        return DeviceConfigurationMapper.toResponseDTO(config);
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceConfigurationResponseDTO getConfiguration(Long outerDeviceId) {
        getValidatedDevice(outerDeviceId);

        DeviceConfiguration config = configRepository.findByOuterDeviceId(outerDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration not found for this device"));

        return DeviceConfigurationMapper.toResponseDTO(config);
    }

    @Override
    @Transactional
    public DeviceConfigurationResponseDTO updateConfiguration(Long outerDeviceId, UpdateDeviceConfigurationRequestDTO dto) {
        Device device = getValidatedDevice(outerDeviceId);

        DeviceConfiguration config = configRepository.findByOuterDeviceId(outerDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration not found for this device"));

        config.setConfigurationStatus("OUTDATED");
        config.setConfigurationVersion(config.getConfigurationVersion() + 1);

        String plainTextPassword = null;
        boolean updateMqtt = false;

        if (dto.getWifiSsid() != null && !dto.getWifiSsid().isEmpty()) {
            config.setWifiSsid(dto.getWifiSsid());
            updateMqtt = true;
        }

        if (dto.getWifiPassword() != null && !dto.getWifiPassword().isEmpty()) {
            plainTextPassword = dto.getWifiPassword();
            config.setWifiPassword(encryptionService.encrypt(plainTextPassword));
            updateMqtt = true;
        } else if (updateMqtt || dto.getInnerDeviceId() != null || dto.getPenDeviceId() != null || dto.getGlucometerDeviceId() != null) {
            // Need plain text password to re-publish MQTT config update
            plainTextPassword = encryptionService.decrypt(config.getWifiPassword());
            updateMqtt = true;
        }

        if (dto.getInnerDeviceId() != null) {
            config.setInnerDeviceId(dto.getInnerDeviceId());
            updateMqtt = true;
        }

        if (dto.getPenDeviceId() != null) {
            config.setPenDeviceId(dto.getPenDeviceId());
            updateMqtt = true;
        }

        if (dto.getGlucometerDeviceId() != null) {
            config.setGlucometerDeviceId(dto.getGlucometerDeviceId());
            updateMqtt = true;
        }

        if (updateMqtt) {
            config.setConfigurationStatus("PENDING");
            config = configRepository.save(config);
            publishConfigCommand(device, config, plainTextPassword);
        }

        return DeviceConfigurationMapper.toResponseDTO(config);
    }

    private void publishConfigCommand(Device device, DeviceConfiguration config, String plainTextPassword) {
        DeviceCommand command = new DeviceCommand();
        command.setDeviceId(device.getDeviceId());
        command.setPatientId(device.getPatientId());
        command.setCommandType("CONFIG_UPDATE");
        command.setCommandStatus("PENDING");

        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("type", "CONFIG_UPDATE");
            payloadMap.put("configurationVersion", config.getConfigurationVersion());

            Map<String, String> wifiMap = new HashMap<>();
            wifiMap.put("ssid", config.getWifiSsid());
            wifiMap.put("password", plainTextPassword);
            payloadMap.put("wifi", wifiMap);

            Map<String, Long> deviceMappingMap = new HashMap<>();
            deviceMappingMap.put("innerDeviceId", config.getInnerDeviceId());
            deviceMappingMap.put("penDeviceId", config.getPenDeviceId());
            deviceMappingMap.put("glucometerDeviceId", config.getGlucometerDeviceId());
            payloadMap.put("deviceMapping", deviceMappingMap);

            // Adding timestamp as requested by user
            payloadMap.put("timestamp", java.time.Instant.now().toString());

            String payloadJson = objectMapper.writeValueAsString(payloadMap);
            command.setPayload(payloadJson);
            command = commandRepository.save(command);

            // Add commandId to payload for ACK correlation
            payloadMap.put("commandId", command.getCommandId());
            payloadJson = objectMapper.writeValueAsString(payloadMap);
            
            // Re-save command with updated payload
            command.setPayload(payloadJson);
            command = commandRepository.save(command);

            String topic = "diasmart/v1/devices/" + device.getDeviceUid() + "/commands";
            mqttService.publish(topic, payloadJson);

            command.setCommandStatus("SENT");
            commandRepository.save(command);

            config.setConfigurationStatus("SENT");
            configRepository.save(config);

        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize command payload", "PAYLOAD_SERIALIZATION_ERROR");
        } catch (Exception e) {
            command.setCommandStatus("FAILED");
            commandRepository.save(command);

            config.setConfigurationStatus("FAILED");
            configRepository.save(config);

            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to publish MQTT command", "MQTT_PUBLISH_ERROR");
        }
    }
}
