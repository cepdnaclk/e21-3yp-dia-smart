package com.diasmart.springapi.deviceconfig.service.impl;

import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.deviceconfig.dto.CreateDeviceConfigurationRequestDTO;
import com.diasmart.springapi.deviceconfig.dto.DeviceConfigurationResponseDTO;
import com.diasmart.springapi.deviceconfig.dto.UpdateDeviceConfigurationRequestDTO;
import com.diasmart.springapi.deviceconfig.entity.DeviceCommand;
import com.diasmart.springapi.deviceconfig.entity.DeviceConfiguration;
import com.diasmart.springapi.deviceconfig.repository.DeviceCommandRepository;
import com.diasmart.springapi.deviceconfig.repository.DeviceConfigurationRepository;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.mqtt.service.MqttService;
import com.diasmart.springapi.relationships.service.PatientAccessService;
import com.diasmart.springapi.shared.security.EncryptionService;
import com.diasmart.springapi.shared.security.EncryptionService.EncryptedPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceConfigurationServiceImplTest {

    @Mock
    private DeviceConfigurationRepository configRepository;

    @Mock
    private DeviceCommandRepository commandRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private PatientAccessService patientAccessService;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private MqttService mqttService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DeviceConfigurationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeviceConfigurationServiceImpl(
                configRepository,
                commandRepository,
                deviceRepository,
                patientAccessService,
                encryptionService,
                mqttService,
                objectMapper
        );
    }

    @Test
    void createConfigurationShouldSaveEncryptedPasswordAndPublishCommand() throws Exception {
        Device device = createOuterGateway();
        CreateDeviceConfigurationRequestDTO dto = new CreateDeviceConfigurationRequestDTO();
        dto.setOuterDeviceId(1L);
        dto.setWifiSsid("Dialog Home");
        dto.setWifiPassword("12345678");

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(configRepository.existsByOuterDeviceId(1L)).thenReturn(false);
        when(encryptionService.encryptStructured("12345678")).thenReturn(encryptedPayload("cipher-one"));
        when(configRepository.save(any(DeviceConfiguration.class))).thenAnswer(invocation -> {
            DeviceConfiguration config = invocation.getArgument(0);
            if (config.getConfigurationId() == null) {
                config.setConfigurationId(11L);
            }
            return config;
        });
        when(commandRepository.save(any(DeviceCommand.class))).thenAnswer(invocation -> {
            DeviceCommand command = invocation.getArgument(0);
            if (command.getCommandId() == null) {
                command.setCommandId(25L);
            }
            return command;
        });

        DeviceConfigurationResponseDTO response = service.createConfiguration(dto);

        assertEquals(1L, response.getOuterDeviceId());
        assertEquals("Dialog Home", response.getWifiSsid());
        assertEquals("PUBLISHED", response.getConfigurationStatus());
        assertResponseDoesNotExposePassword();

        ArgumentCaptor<DeviceConfiguration> configCaptor = ArgumentCaptor.forClass(DeviceConfiguration.class);
        verify(configRepository, atLeastOnce()).save(configCaptor.capture());
        assertEquals(encoded("cipher-one"), configCaptor.getValue().getWifiPasswordCiphertext());

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService).publish(eq("diasmart/devices/OUT-001/commands"), payloadCaptor.capture(), eq(1), eq(false));

        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
        assertEquals("WIFI_CONFIGURATION", payload.get("commandType").asText());
        assertEquals("CMD-25", payload.get("commandId").asText());
        assertEquals("Dialog Home", payload.get("payload").get("wifiSsid").asText());
        assertEquals("12345678", payload.get("payload").get("wifiPassword").asText());
    }

    @Test
    void createConfigurationShouldRejectExistingConfiguration() {
        Device device = createOuterGateway();
        CreateDeviceConfigurationRequestDTO dto = new CreateDeviceConfigurationRequestDTO();
        dto.setOuterDeviceId(1L);
        dto.setWifiSsid("Dialog Home");
        dto.setWifiPassword("12345678");

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(configRepository.existsByOuterDeviceId(1L)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> service.createConfiguration(dto));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("CONFIG_ALREADY_EXISTS", exception.getErrorCode());
        verify(mqttService, never()).publish(any(), any(), anyInt(), anyBoolean());
    }

    @Test
    void getConfigurationShouldReturnConfigurationWithoutPassword() {
        Device device = createOuterGateway();
        DeviceConfiguration config = createConfiguration();

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(configRepository.findByOuterDeviceId(1L)).thenReturn(Optional.of(config));

        DeviceConfigurationResponseDTO response = service.getConfiguration(1L);

        assertEquals(1L, response.getOuterDeviceId());
        assertEquals("Dialog Home", response.getWifiSsid());
        assertResponseDoesNotExposePassword();
        verify(mqttService, never()).publish(any(), any(), anyInt(), anyBoolean());
    }

    @Test
    void getConfigurationShouldRejectInactiveOuterDevice() {
        Device device = createOuterGateway();
        device.setActive(false);

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));

        ApiException exception = assertThrows(ApiException.class, () -> service.getConfiguration(1L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("DEVICE_INACTIVE", exception.getErrorCode());
        verify(configRepository, never()).findByOuterDeviceId(any());
    }

    @Test
    void updateConfigurationShouldUpdatePasswordAndAutoPublish() throws Exception {
        Device device = createOuterGateway();
        DeviceConfiguration config = createConfiguration();
        UpdateDeviceConfigurationRequestDTO dto = new UpdateDeviceConfigurationRequestDTO();
        dto.setWifiSsid("Fiber Home");
        dto.setWifiPassword("newpass123");

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(configRepository.findByOuterDeviceId(1L)).thenReturn(Optional.of(config));
        when(encryptionService.encryptStructured("newpass123")).thenReturn(encryptedPayload("cipher-two"));
        when(configRepository.save(any(DeviceConfiguration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(commandRepository.save(any(DeviceCommand.class))).thenAnswer(invocation -> {
            DeviceCommand command = invocation.getArgument(0);
            if (command.getCommandId() == null) {
                command.setCommandId(26L);
            }
            return command;
        });

        DeviceConfigurationResponseDTO response = service.updateConfiguration(1L, dto);

        assertEquals("Fiber Home", response.getWifiSsid());
        assertEquals(2, response.getConfigurationVersion());
        assertEquals("PUBLISHED", response.getConfigurationStatus());
        assertEquals(encoded("cipher-two"), config.getWifiPasswordCiphertext());

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService).publish(eq("diasmart/devices/OUT-001/commands"), payloadCaptor.capture(), eq(1), eq(false));

        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
        assertEquals("CMD-26", payload.get("commandId").asText());
        assertEquals(2, payload.get("payload").get("configurationVersion").asInt());
        assertEquals("Fiber Home", payload.get("payload").get("wifiSsid").asText());
        assertEquals("newpass123", payload.get("payload").get("wifiPassword").asText());
    }

    @Test
    void updateConfigurationWithNoChangesShouldNotPublishOrIncrementVersion() {
        Device device = createOuterGateway();
        DeviceConfiguration config = createConfiguration();
        config.setConfigurationStatus("APPLIED");
        UpdateDeviceConfigurationRequestDTO dto = new UpdateDeviceConfigurationRequestDTO();

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(configRepository.findByOuterDeviceId(1L)).thenReturn(Optional.of(config));

        DeviceConfigurationResponseDTO response = service.updateConfiguration(1L, dto);

        assertEquals(1, response.getConfigurationVersion());
        assertEquals("APPLIED", response.getConfigurationStatus());
        verify(configRepository, never()).save(any());
        verify(commandRepository, never()).save(any());
        verify(mqttService, never()).publish(any(), any(), anyInt(), anyBoolean());
    }

    private Device createOuterGateway() {
        Device device = new Device();
        device.setDeviceId(1L);
        device.setPatientId(10L);
        device.setDeviceUid("OUT-001");
        device.setDeviceType("OUTER_GATEWAY");
        device.setActive(true);
        return device;
    }

    private DeviceConfiguration createConfiguration() {
        DeviceConfiguration config = new DeviceConfiguration();
        config.setConfigurationId(11L);
        config.setOuterDeviceId(1L);
        config.setPatientId(10L);
        config.setWifiSsid("Dialog Home");
        config.setWifiPassword("encrypted-password");
        config.setConfigurationVersion(1);
        config.setConfigurationStatus("SENT");
        return config;
    }

    private EncryptedPayload encryptedPayload(String ciphertext) {
        return new EncryptedPayload(
                encoded(ciphertext),
                encoded("nonce-123456"),
                encoded("tag-123456789012")
        );
    }

    private String encoded(String value) {
        return java.util.Base64.getEncoder().encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void assertResponseDoesNotExposePassword() {
        assertFalse(Arrays.stream(DeviceConfigurationResponseDTO.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("wifiPassword")));
    }
}
