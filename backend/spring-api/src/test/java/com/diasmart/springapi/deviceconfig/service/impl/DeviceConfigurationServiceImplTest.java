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

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
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

    @Mock
    private AfterCommitExecutor afterCommitExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private WifiCommandPublishProperties publishProperties;
    private WifiCommandStateService wifiCommandStateService;
    private WifiConfigurationCommandPublisher wifiCommandPublisher;
    private DeviceConfigurationServiceImpl service;

    @BeforeEach
    void setUp() {
        publishProperties = new WifiCommandPublishProperties();
        wifiCommandStateService = new WifiCommandStateService(
                commandRepository,
                configRepository,
                deviceRepository,
                publishProperties
        );
        wifiCommandPublisher = new WifiConfigurationCommandPublisher(
                wifiCommandStateService,
                encryptionService,
                mqttService,
                objectMapper
        );
        lenient().doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(afterCommitExecutor).runAfterCommit(any(Runnable.class));
        service = new DeviceConfigurationServiceImpl(
                configRepository,
                commandRepository,
                deviceRepository,
                patientAccessService,
                encryptionService,
                wifiCommandPublisher,
                wifiCommandStateService,
                afterCommitExecutor,
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
        when(encryptionService.decryptStructured(
                encoded("cipher-one"),
                encoded("nonce-123456"),
                encoded("tag-123456789012")
        )).thenReturn("12345678");
        DeviceConfiguration[] savedConfig = new DeviceConfiguration[1];
        when(configRepository.save(any(DeviceConfiguration.class))).thenAnswer(invocation -> {
            DeviceConfiguration config = invocation.getArgument(0);
            if (config.getConfigurationId() == null) {
                config.setConfigurationId(11L);
            }
            savedConfig[0] = config;
            return config;
        });
        when(configRepository.findByConfigurationId(11L)).thenAnswer(invocation -> Optional.ofNullable(savedConfig[0]));
        stubCommandSaveAndFind(25L);

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

        ArgumentCaptor<DeviceCommand> commandCaptor = ArgumentCaptor.forClass(DeviceCommand.class);
        verify(commandRepository, atLeast(2)).save(commandCaptor.capture());
        DeviceCommand persistedCommand = lastCapturedCommand(commandCaptor);
        assertEquals(11L, persistedCommand.getDeviceConfigurationId());
        assertEquals(1, persistedCommand.getConfigurationVersion());
        assertPersistedWifiCommandPayloadSafe(
                persistedCommand,
                "12345678",
                11L,
                1,
                encoded("cipher-one"),
                encoded("nonce-123456"),
                encoded("tag-123456789012")
        );
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
    void createConfigurationShouldSchedulePublishAfterCommit() {
        Device device = createOuterGateway();
        CreateDeviceConfigurationRequestDTO dto = new CreateDeviceConfigurationRequestDTO();
        dto.setOuterDeviceId(1L);
        dto.setWifiSsid("Dialog Home");
        dto.setWifiPassword("12345678");

        doNothing().when(afterCommitExecutor).runAfterCommit(any(Runnable.class));
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
        stubCommandSaveOnly(25L);

        DeviceConfigurationResponseDTO response = service.createConfiguration(dto);

        assertEquals("PENDING", response.getConfigurationStatus());
        verify(afterCommitExecutor).runAfterCommit(any(Runnable.class));
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
        when(encryptionService.decryptStructured(
                encoded("cipher-two"),
                encoded("nonce-123456"),
                encoded("tag-123456789012")
        )).thenReturn("newpass123");
        when(configRepository.save(any(DeviceConfiguration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(configRepository.findByConfigurationId(11L)).thenReturn(Optional.of(config));
        stubCommandSaveAndFind(26L);

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
        verify(commandRepository).expireSupersededWifiCommands(11L, 2);

        ArgumentCaptor<DeviceCommand> commandCaptor = ArgumentCaptor.forClass(DeviceCommand.class);
        verify(commandRepository, atLeast(2)).save(commandCaptor.capture());
        DeviceCommand persistedCommand = lastCapturedCommand(commandCaptor);
        assertEquals(11L, persistedCommand.getDeviceConfigurationId());
        assertEquals(2, persistedCommand.getConfigurationVersion());
        assertPersistedWifiCommandPayloadSafe(
                persistedCommand,
                "newpass123",
                11L,
                2,
                encoded("cipher-two"),
                encoded("nonce-123456"),
                encoded("tag-123456789012")
        );
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

    @Test
    void sendConfigurationShouldCreateManualResendCommandForCurrentVersion() throws Exception {
        Device device = createOuterGateway();
        DeviceConfiguration config = createConfiguration();
        config.setConfigurationVersion(3);
        config.setWifiPasswordCiphertext(encoded("stored-cipher"));
        config.setWifiPasswordNonce(encoded("stored-nonce"));
        config.setWifiPasswordTag(encoded("stored-tag"));

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(configRepository.findByOuterDeviceId(1L)).thenReturn(Optional.of(config));
        when(configRepository.save(any(DeviceConfiguration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(configRepository.findByConfigurationId(11L)).thenReturn(Optional.of(config));
        stubCommandSaveAndFind(31L);
        when(encryptionService.decryptStructured(
                encoded("stored-cipher"),
                encoded("stored-nonce"),
                encoded("stored-tag")
        )).thenReturn("stored-pass123");

        DeviceConfigurationResponseDTO response = service.sendConfiguration(1L);

        assertEquals(3, response.getConfigurationVersion());
        assertEquals("PUBLISHED", response.getConfigurationStatus());

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService).publish(eq("diasmart/devices/OUT-001/commands"), payloadCaptor.capture(), eq(1), eq(false));

        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
        assertEquals("CMD-31", payload.get("commandId").asText());
        assertEquals(3, payload.get("payload").get("configurationVersion").asInt());
        assertEquals("stored-pass123", payload.get("payload").get("wifiPassword").asText());

        ArgumentCaptor<DeviceCommand> commandCaptor = ArgumentCaptor.forClass(DeviceCommand.class);
        verify(commandRepository, atLeast(2)).save(commandCaptor.capture());
        DeviceCommand persistedCommand = lastCapturedCommand(commandCaptor);
        assertEquals(11L, persistedCommand.getDeviceConfigurationId());
        assertEquals(3, persistedCommand.getConfigurationVersion());
        assertPersistedWifiCommandPayloadSafe(
                persistedCommand,
                "stored-pass123",
                11L,
                3,
                encoded("stored-cipher"),
                encoded("stored-nonce"),
                encoded("stored-tag")
        );
    }

    @Test
    void publishWifiCommandShouldBuildPayloadFromStoredConfiguration() throws Exception {
        Device outerDevice = createOuterGateway();
        Device innerDevice = createInnerUnit();
        DeviceConfiguration config = createConfiguration();
        config.setInnerDeviceId(2L);
        config.setWifiPasswordCiphertext(encoded("stored-cipher"));
        config.setWifiPasswordNonce(encoded("stored-nonce"));
        config.setWifiPasswordTag(encoded("stored-tag"));
        DeviceCommand command = createWifiCommand(25L, config);

        when(commandRepository.findById(25L)).thenReturn(Optional.of(command));
        when(configRepository.findByConfigurationId(11L)).thenReturn(Optional.of(config));
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(outerDevice));
        when(deviceRepository.findById(2L)).thenReturn(Optional.of(innerDevice));
        stubClaim(command, 25L);
        when(encryptionService.decryptStructured(
                encoded("stored-cipher"),
                encoded("stored-nonce"),
                encoded("stored-tag")
        )).thenReturn("stored-pass123");

        wifiCommandPublisher.publishWifiCommand(25L);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService).publish(eq("diasmart/devices/OUT-001/commands"), payloadCaptor.capture(), eq(1), eq(false));

        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
        assertEquals("CMD-25", payload.get("commandId").asText());
        assertEquals("OUT-001", payload.get("outerDeviceId").asText());
        assertEquals("INNER-001", payload.get("payload").get("innerDeviceId").asText());
        assertEquals(2, payload.get("payload").get("innerDeviceNumericId").asInt());
        assertEquals(1, payload.get("payload").get("configurationVersion").asInt());
        assertEquals("Dialog Home", payload.get("payload").get("wifiSsid").asText());
        assertEquals("stored-pass123", payload.get("payload").get("wifiPassword").asText());
        assertPersistedWifiCommandPayloadSafe(
                command,
                "stored-pass123",
                11L,
                1,
                encoded("stored-cipher"),
                encoded("stored-nonce"),
                encoded("stored-tag")
        );
    }

    @Test
    void publishWifiCommandShouldRejectConfigurationVersionMismatch() {
        DeviceConfiguration config = createConfiguration();
        config.setConfigurationVersion(2);
        DeviceCommand command = createWifiCommand(25L, config);
        command.setConfigurationVersion(1);

        when(commandRepository.findById(25L)).thenReturn(Optional.of(command));
        when(configRepository.findByConfigurationId(11L)).thenReturn(Optional.of(config));
        stubClaim(command, 25L);

        ApiException exception = assertThrows(ApiException.class, () -> wifiCommandPublisher.publishWifiCommand(25L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("CONFIGURATION_VERSION_MISMATCH", exception.getErrorCode());
        assertEquals("FAILED", command.getCommandStatus());
        assertEquals("CONFIGURATION_VERSION_MISMATCH", command.getLastError());
        assertEquals(3, command.getRetryCount());
        verify(mqttService, never()).publish(any(), any(), anyInt(), anyBoolean());
        verify(encryptionService, never()).decryptStructured(anyString(), anyString(), anyString());
    }

    @Test
    void publishWifiCommandShouldHandleMissingConfigurationSafely() {
        DeviceConfiguration config = createConfiguration();
        DeviceCommand command = createWifiCommand(25L, config);

        when(commandRepository.findById(25L)).thenReturn(Optional.of(command));
        when(configRepository.findByConfigurationId(11L)).thenReturn(Optional.empty());
        stubClaim(command, 25L);

        ApiException exception = assertThrows(ApiException.class, () -> wifiCommandPublisher.publishWifiCommand(25L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("CONFIGURATION_NOT_FOUND", exception.getErrorCode());
        assertEquals("FAILED", command.getCommandStatus());
        assertEquals("CONFIGURATION_NOT_FOUND", command.getLastError());
        assertEquals(3, command.getRetryCount());
        verify(mqttService, never()).publish(any(), any(), anyInt(), anyBoolean());
        verify(encryptionService, never()).decryptStructured(anyString(), anyString(), anyString());
    }

    @Test
    void publishWifiCommandShouldRejectInactiveOuterDevice() {
        Device outerDevice = createOuterGateway();
        outerDevice.setActive(false);
        DeviceConfiguration config = createConfiguration();
        DeviceCommand command = createWifiCommand(25L, config);

        when(commandRepository.findById(25L)).thenReturn(Optional.of(command));
        when(configRepository.findByConfigurationId(11L)).thenReturn(Optional.of(config));
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(outerDevice));
        stubClaim(command, 25L);

        ApiException exception = assertThrows(ApiException.class, () -> wifiCommandPublisher.publishWifiCommand(25L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("DEVICE_INACTIVE", exception.getErrorCode());
        assertEquals("FAILED", command.getCommandStatus());
        assertEquals("DEVICE_INACTIVE", command.getLastError());
        assertEquals(3, command.getRetryCount());
        verify(mqttService, never()).publish(any(), any(), anyInt(), anyBoolean());
        verify(encryptionService, never()).decryptStructured(anyString(), anyString(), anyString());
    }

    @Test
    void publishWifiCommandShouldPersistRetryableFailureWhenMqttFails() throws Exception {
        Device outerDevice = createOuterGateway();
        DeviceConfiguration config = createConfiguration();
        config.setWifiPasswordCiphertext(encoded("stored-cipher"));
        config.setWifiPasswordNonce(encoded("stored-nonce"));
        config.setWifiPasswordTag(encoded("stored-tag"));
        DeviceCommand command = createWifiCommand(25L, config);

        when(commandRepository.findById(25L)).thenReturn(Optional.of(command));
        when(configRepository.findByConfigurationId(11L)).thenReturn(Optional.of(config));
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(outerDevice));
        stubClaim(command, 25L);
        when(encryptionService.decryptStructured(
                encoded("stored-cipher"),
                encoded("stored-nonce"),
                encoded("stored-tag")
        )).thenReturn("stored-pass123");
        doThrow(new RuntimeException("broker unavailable"))
                .when(mqttService)
                .publish(eq("diasmart/devices/OUT-001/commands"), anyString(), eq(1), eq(false));

        ApiException exception = assertThrows(ApiException.class, () -> wifiCommandPublisher.publishWifiCommand(25L));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertEquals("MQTT_PUBLISH_ERROR", exception.getErrorCode());
        assertEquals("FAILED", command.getCommandStatus());
        assertEquals(1, command.getRetryCount());
        assertEquals("MQTT_PUBLISH_FAILED", command.getLastError());
        assertNotNull(command.getLastAttemptAt());
        assertNotNull(command.getNextRetryAt());
        assertEquals("FAILED", config.getConfigurationStatus());
        assertPersistedWifiCommandPayloadSafe(command, "stored-pass123", 11L, 1, encoded("stored-cipher"));
    }

    @Test
    void publishWifiCommandShouldNotPublishWhenClaimFailsAtRetryLimit() {
        DeviceConfiguration config = createConfiguration();
        DeviceCommand command = createWifiCommand(25L, config);
        command.setRetryCount(3);

        when(commandRepository.claimRecoverableWifiCommand(eq(25L), any(), any(), anyInt())).thenReturn(0);

        assertFalse(wifiCommandPublisher.publishWifiCommand(25L));

        verify(commandRepository, never()).findById(25L);
        verify(mqttService, never()).publish(any(), any(), anyInt(), anyBoolean());
        verify(encryptionService, never()).decryptStructured(anyString(), anyString(), anyString());
    }

    @Test
    void recoverySchedulerShouldPublishEligibleCommands() {
        Device outerDevice = createOuterGateway();
        DeviceConfiguration config = createConfiguration();
        config.setWifiPasswordCiphertext(encoded("stored-cipher"));
        config.setWifiPasswordNonce(encoded("stored-nonce"));
        config.setWifiPasswordTag(encoded("stored-tag"));
        DeviceCommand command = createWifiCommand(25L, config);

        when(commandRepository.findRecoverableWifiCommandIds(any(), any(), anyInt(), any()))
                .thenReturn(List.of(25L));
        when(commandRepository.findById(25L)).thenReturn(Optional.of(command));
        when(configRepository.findByConfigurationId(11L)).thenReturn(Optional.of(config));
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(outerDevice));
        stubClaim(command, 25L);
        when(encryptionService.decryptStructured(
                encoded("stored-cipher"),
                encoded("stored-nonce"),
                encoded("stored-tag")
        )).thenReturn("stored-pass123");

        WifiCommandRecoveryScheduler scheduler = new WifiCommandRecoveryScheduler(wifiCommandStateService, wifiCommandPublisher);

        scheduler.recoverPendingWifiCommands();

        verify(mqttService).publish(eq("diasmart/devices/OUT-001/commands"), anyString(), eq(1), eq(false));
        assertEquals("PUBLISHED", command.getCommandStatus());
    }

    @Test
    void nonWifiCommandShouldRetainGenericPayloadBehavior() {
        DeviceCommand command = new DeviceCommand();
        command.setCommandType("CARE_PLAN");
        command.setPayload("{\"carePlanId\":\"CP-1\",\"schedules\":[]}");

        assertEquals("{\"carePlanId\":\"CP-1\",\"schedules\":[]}", command.getPayload());
    }

    @Test
    void requestDtoToStringShouldNotExposePassword() {
        CreateDeviceConfigurationRequestDTO createDto = new CreateDeviceConfigurationRequestDTO();
        createDto.setWifiPassword("secret123");
        UpdateDeviceConfigurationRequestDTO updateDto = new UpdateDeviceConfigurationRequestDTO();
        updateDto.setWifiPassword("secret456");

        assertFalse(createDto.toString().contains("secret123"));
        assertFalse(updateDto.toString().contains("secret456"));
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

    private Device createInnerUnit() {
        Device device = new Device();
        device.setDeviceId(2L);
        device.setPatientId(10L);
        device.setDeviceUid("INNER-001");
        device.setDeviceType("INNER_UNIT");
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
        command.setPayload("{\"configurationId\":11,\"configurationVersion\":1,\"innerDeviceId\":2}");
        return command;
    }

    private void stubCommandSaveAndFind(Long commandId) {
        DeviceCommand[] savedCommand = new DeviceCommand[1];
        when(commandRepository.save(any(DeviceCommand.class))).thenAnswer(invocation -> {
            DeviceCommand command = invocation.getArgument(0);
            if (command.getCommandId() == null) {
                command.setCommandId(commandId);
            }
            savedCommand[0] = command;
            return command;
        });
        when(commandRepository.findById(commandId)).thenAnswer(invocation -> Optional.ofNullable(savedCommand[0]));
        when(commandRepository.claimRecoverableWifiCommand(eq(commandId), any(), any(), anyInt())).thenAnswer(invocation -> {
            DeviceCommand command = savedCommand[0];
            if (command == null) {
                return 0;
            }
            OffsetDateTime now = invocation.getArgument(1);
            command.setCommandStatus("SENT");
            command.setLastAttemptAt(now);
            command.setLastError(null);
            return 1;
        });
    }

    private void stubCommandSaveOnly(Long commandId) {
        when(commandRepository.save(any(DeviceCommand.class))).thenAnswer(invocation -> {
            DeviceCommand command = invocation.getArgument(0);
            if (command.getCommandId() == null) {
                command.setCommandId(commandId);
            }
            return command;
        });
    }

    private void stubClaim(DeviceCommand command, Long commandId) {
        when(commandRepository.claimRecoverableWifiCommand(eq(commandId), any(), any(), anyInt())).thenAnswer(invocation -> {
            OffsetDateTime now = invocation.getArgument(1);
            command.setCommandStatus("SENT");
            command.setLastAttemptAt(now);
            command.setLastError(null);
            return 1;
        });
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

    private DeviceCommand lastCapturedCommand(ArgumentCaptor<DeviceCommand> commandCaptor) {
        return commandCaptor.getAllValues().get(commandCaptor.getAllValues().size() - 1);
    }

    private void assertPersistedWifiCommandPayloadSafe(
            DeviceCommand command,
            String plainTextPassword,
            Long configurationId,
            int configurationVersion,
            String... encryptedPasswordParts
    ) throws Exception {
        assertNotNull(command.getPayload());
        assertFalse(command.getPayload().contains(plainTextPassword));
        assertFalse(command.getPayload().contains("wifiPassword"));
        assertFalse(command.getPayload().contains("wifiPasswordCiphertext"));
        assertFalse(command.getPayload().contains("wifiPasswordNonce"));
        assertFalse(command.getPayload().contains("wifiPasswordTag"));
        for (String encryptedPart : encryptedPasswordParts) {
            assertFalse(command.getPayload().contains(encryptedPart));
        }

        JsonNode storedPayload = objectMapper.readTree(command.getPayload());
        assertEquals(configurationId, storedPayload.get("configurationId").asLong());
        assertEquals(configurationVersion, storedPayload.get("configurationVersion").asInt());
        assertFalse(storedPayload.has("wifiPassword"));
    }
}
