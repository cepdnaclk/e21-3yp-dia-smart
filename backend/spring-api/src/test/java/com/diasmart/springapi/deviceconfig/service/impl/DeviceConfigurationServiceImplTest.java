package com.diasmart.springapi.deviceconfig.service.impl;

import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.deviceconfig.dto.CreateDeviceConfigurationRequestDTO;
import com.diasmart.springapi.deviceconfig.dto.DeviceConfigurationResponseDTO;
import com.diasmart.springapi.deviceconfig.dto.UpdateDeviceConfigurationRequestDTO;
import com.diasmart.springapi.deviceconfig.entity.DeviceCommand;
import com.diasmart.springapi.deviceconfig.entity.DeviceCommandAcknowledgement;
import com.diasmart.springapi.deviceconfig.entity.DeviceConfiguration;
import com.diasmart.springapi.deviceconfig.repository.DeviceCommandAcknowledgementRepository;
import com.diasmart.springapi.deviceconfig.repository.DeviceCommandRepository;
import com.diasmart.springapi.deviceconfig.repository.DeviceConfigurationRepository;
import com.diasmart.springapi.deviceevents.entity.DeviceTelemetryEvent;
import com.diasmart.springapi.deviceevents.repository.DeviceTelemetryEventRepository;
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

import java.time.OffsetDateTime;
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
    private DeviceCommandAcknowledgementRepository acknowledgementRepository;

    @Mock
    private DeviceTelemetryEventRepository telemetryEventRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private PatientAccessService patientAccessService;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private MqttService mqttService;

    @Mock
    private AfterCommitExecutor afterCommitExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DeviceConfigurationServiceImpl service;

    @BeforeEach
    void setUp() {
        WifiCommandPublishProperties publishProperties = new WifiCommandPublishProperties();
        DeviceProvisioningProperties provisioningProperties = new DeviceProvisioningProperties();
        WifiCommandStateService wifiCommandStateService = new WifiCommandStateService(
                commandRepository,
                configRepository,
                deviceRepository,
                publishProperties,
                provisioningProperties
        );
        WifiConfigurationCommandPublisher wifiCommandPublisher = new WifiConfigurationCommandPublisher(
                wifiCommandStateService,
                encryptionService,
                mqttService,
                objectMapper
        );
        DeviceProvisioningLifecycleService lifecycleService = new DeviceProvisioningLifecycleService();

        lenient().doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(afterCommitExecutor).runAfterCommit(any(Runnable.class));

        service = new DeviceConfigurationServiceImpl(
                configRepository,
                commandRepository,
                acknowledgementRepository,
                telemetryEventRepository,
                deviceRepository,
                patientAccessService,
                encryptionService,
                wifiCommandPublisher,
                wifiCommandStateService,
                lifecycleService,
                afterCommitExecutor,
                objectMapper
        );
    }

    @Test
    void createConfigurationShouldStoreEncryptedPasswordAndPublishSafeCommand() throws Exception {
        Device outer = createOuterGateway();
        CreateDeviceConfigurationRequestDTO dto = new CreateDeviceConfigurationRequestDTO();
        dto.setOuterDeviceId(1L);
        dto.setWifiSsid("Dialog Home");
        dto.setWifiPassword("12345678");

        DeviceConfiguration[] savedConfig = new DeviceConfiguration[1];
        DeviceCommand[] savedCommand = new DeviceCommand[1];

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(outer));
        when(configRepository.existsByOuterDeviceId(1L)).thenReturn(false);
        when(encryptionService.encryptStructured("12345678")).thenReturn(encryptedPayload("cipher-one"));
        when(encryptionService.decryptStructured(
                encoded("cipher-one"),
                encoded("nonce-123456"),
                encoded("tag-123456789012")
        )).thenReturn("12345678");
        when(configRepository.save(any(DeviceConfiguration.class))).thenAnswer(invocation -> {
            DeviceConfiguration config = invocation.getArgument(0);
            if (config.getConfigurationId() == null) {
                config.setConfigurationId(11L);
            }
            savedConfig[0] = config;
            return config;
        });
        when(configRepository.findByConfigurationId(11L)).thenAnswer(invocation -> Optional.ofNullable(savedConfig[0]));
        when(commandRepository.save(any(DeviceCommand.class))).thenAnswer(invocation -> {
            DeviceCommand command = invocation.getArgument(0);
            if (command.getCommandId() == null) {
                command.setCommandId(25L);
            }
            savedCommand[0] = command;
            return command;
        });
        when(commandRepository.findById(25L)).thenAnswer(invocation -> Optional.ofNullable(savedCommand[0]));
        when(commandRepository.claimRecoverableWifiCommand(eq(25L), any(), any(), anyInt())).thenAnswer(invocation -> {
            savedCommand[0].setCommandStatus("SENT");
            savedCommand[0].setLastAttemptAt(invocation.getArgument(1));
            return 1;
        });

        DeviceConfigurationResponseDTO response = service.createConfiguration(dto);

        assertEquals("PUBLISHED", response.getConfigurationStatus());
        assertEquals(encoded("cipher-one"), savedConfig[0].getWifiPasswordCiphertext());
        assertEquals("CMD-25", savedConfig[0].getLastProvisioningCommandUid());
        assertResponseDoesNotExposePassword();

        ArgumentCaptor<String> mqttPayloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService).publish(eq("diasmart/devices/OUT-001/commands"), mqttPayloadCaptor.capture(), eq(1), eq(false));

        JsonNode mqttPayload = objectMapper.readTree(mqttPayloadCaptor.getValue());
        assertEquals("CMD-25", mqttPayload.get("commandId").asText());
        assertEquals("WIFI_CONFIGURATION", mqttPayload.get("commandType").asText());
        assertEquals("12345678", mqttPayload.get("payload").get("wifiPassword").asText());

        assertNotNull(savedCommand[0].getPayload());
        assertFalse(savedCommand[0].getPayload().contains("12345678"));
        assertFalse(savedCommand[0].getPayload().contains("wifiPassword"));
        assertEquals(11L, objectMapper.readTree(savedCommand[0].getPayload()).get("configurationId").asLong());
    }

    @Test
    void updateConfigurationWithNoChangesShouldNotCreateCommandsOrPublish() {
        Device outer = createOuterGateway();
        DeviceConfiguration config = createConfiguration();
        config.setConfigurationStatus("APPLIED");

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(outer));
        when(configRepository.findByOuterDeviceId(1L)).thenReturn(Optional.of(config));

        DeviceConfigurationResponseDTO response = service.updateConfiguration(1L, new UpdateDeviceConfigurationRequestDTO());

        assertEquals(1, response.getConfigurationVersion());
        assertEquals("APPLIED", response.getConfigurationStatus());
        verify(configRepository, never()).save(any());
        verify(commandRepository, never()).save(any());
        verify(mqttService, never()).publish(any(), any(), anyInt(), anyBoolean());
    }

    @Test
    void getConfigurationStatusShouldReturnLifecycleFieldsWithoutCredentials() {
        Device outer = createOuterGateway();
        DeviceConfiguration config = createConfiguration();
        config.setConfigurationVersion(3);
        config.setConfigurationStatus("APPLIED");
        config.setOuterUnitStatus("APPLIED");
        config.setInnerUnitStatus("CONNECTED");
        config.setMqttStatus("CONNECTED");
        config.setLastSuccessfulConfigurationVersion(3);
        config.setLastProvisioningCommandId(40L);
        config.setLastProvisioningCommandUid("CMD-40");

        DeviceCommand command = createWifiCommand(40L, config);
        command.setCommandStatus("APPLIED");
        command.setPublishedAt(OffsetDateTime.parse("2026-08-02T12:30:00Z"));
        command.setAcknowledgedAt(OffsetDateTime.parse("2026-08-02T12:31:00Z"));
        command.setCompletedAt(OffsetDateTime.parse("2026-08-02T12:32:00Z"));

        DeviceCommandAcknowledgement acknowledgement = new DeviceCommandAcknowledgement();
        acknowledgement.setCommandId(40L);
        acknowledgement.setAckStatus("APPLIED");
        acknowledgement.setProcessingResult("ACCEPTED");

        DeviceTelemetryEvent innerResult = new DeviceTelemetryEvent();
        innerResult.setProcessingStatus("PROCESSED");
        innerResult.setProcessingResult("ACCEPTED");

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(outer));
        when(configRepository.findByOuterDeviceId(1L)).thenReturn(Optional.of(config));
        when(commandRepository.findTopByDeviceConfigurationIdAndConfigurationVersionAndCommandTypeOrderByCreatedAtDesc(
                11L,
                3,
                "WIFI_CONFIGURATION"
        )).thenReturn(Optional.of(command));
        when(acknowledgementRepository.findTopByCommandIdOrderByAcknowledgedAtDesc(40L)).thenReturn(Optional.of(acknowledgement));
        when(telemetryEventRepository.findTopByDeviceConfigurationIdAndEventTypeOrderByReceivedAtDesc(
                11L,
                "INNER_WIFI_CONFIGURATION_RESULT"
        )).thenReturn(Optional.of(innerResult));

        DeviceConfigurationResponseDTO response = service.getConfigurationStatus(1L);

        assertEquals("OUT-001", response.getOuterDeviceUid());
        assertEquals("CMD-40", response.getCommandId());
        assertEquals("APPLIED", response.getCommandStatus());
        assertEquals("CONNECTED", response.getMqttStatus());
        assertEquals("SUCCEEDED", response.getOverallStatus());
        assertTrue(response.getTerminal());
        assertEquals("ACCEPTED", response.getLastAckProcessingResult());
        assertEquals("ACCEPTED", response.getLastResultProcessingResult());
        assertResponseDoesNotExposePassword();
    }

    @Test
    void getConfigurationStatusShouldEnforcePatientAccessBeforeReadingStatus() {
        Device outer = createOuterGateway();
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(outer));
        doThrow(new ApiException(HttpStatus.FORBIDDEN, "PATIENT_ACCESS_DENIED", "Patient access denied"))
                .when(patientAccessService)
                .requireCanViewPatient(10L);

        ApiException exception = assertThrows(ApiException.class, () -> service.getConfigurationStatus(1L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("PATIENT_ACCESS_DENIED", exception.getErrorCode());
        verify(configRepository, never()).findByOuterDeviceId(any());
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

    private DeviceCommand createWifiCommand(Long commandId, DeviceConfiguration config) {
        DeviceCommand command = new DeviceCommand();
        command.setCommandId(commandId);
        command.setCommandUid("CMD-" + commandId);
        command.setDeviceId(config.getOuterDeviceId());
        command.setPatientId(config.getPatientId());
        command.setDeviceConfigurationId(config.getConfigurationId());
        command.setConfigurationVersion(config.getConfigurationVersion());
        command.setCommandType("WIFI_CONFIGURATION");
        command.setCommandStatus("PENDING");
        command.setPayload("{\"configurationId\":11,\"configurationVersion\":1}");
        return command;
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
                .anyMatch(field -> field.getName().equals("wifiPassword")
                        || field.getName().equals("wifiPasswordCiphertext")
                        || field.getName().equals("wifiPasswordNonce")
                        || field.getName().equals("wifiPasswordTag")));
    }
}
