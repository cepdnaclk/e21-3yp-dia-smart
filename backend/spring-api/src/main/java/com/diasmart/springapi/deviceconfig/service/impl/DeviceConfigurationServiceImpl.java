package com.diasmart.springapi.deviceconfig.service.impl;

import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.deviceconfig.dto.CreateDeviceConfigurationRequestDTO;
import com.diasmart.springapi.deviceconfig.dto.DeviceConfigurationResponseDTO;
import com.diasmart.springapi.deviceconfig.dto.UpdateDeviceConfigurationRequestDTO;
import com.diasmart.springapi.deviceconfig.entity.DeviceCommand;
import com.diasmart.springapi.deviceconfig.entity.DeviceCommandAcknowledgement;
import com.diasmart.springapi.deviceconfig.entity.DeviceConfiguration;
import com.diasmart.springapi.deviceconfig.mapper.DeviceConfigurationMapper;
import com.diasmart.springapi.deviceconfig.repository.DeviceCommandAcknowledgementRepository;
import com.diasmart.springapi.deviceconfig.repository.DeviceCommandRepository;
import com.diasmart.springapi.deviceconfig.repository.DeviceConfigurationRepository;
import com.diasmart.springapi.deviceconfig.service.DeviceConfigurationService;
import com.diasmart.springapi.deviceevents.entity.DeviceTelemetryEvent;
import com.diasmart.springapi.deviceevents.repository.DeviceTelemetryEventRepository;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.relationships.service.PatientAccessService;
import com.diasmart.springapi.shared.exceptions.ResourceNotFoundException;
import com.diasmart.springapi.shared.security.EncryptionService;
import com.diasmart.springapi.shared.security.EncryptionService.EncryptedPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DeviceConfigurationServiceImpl implements DeviceConfigurationService {

    private static final String DEVICE_TYPE_OUTER = "OUTER_GATEWAY";
    private static final String DEVICE_TYPE_INNER = "INNER_UNIT";
    private static final String DEVICE_TYPE_PEN = "DOSE_CAP";
    private static final String DEVICE_TYPE_GLUCOMETER = "GLUCOMETER";
    private static final String COMMAND_TYPE_WIFI_CONFIGURATION = "WIFI_CONFIGURATION";
    private static final String EVENT_TYPE_INNER_WIFI_CONFIGURATION_RESULT = "INNER_WIFI_CONFIGURATION_RESULT";

    private final DeviceConfigurationRepository configRepository;
    private final DeviceCommandRepository commandRepository;
    private final DeviceCommandAcknowledgementRepository acknowledgementRepository;
    private final DeviceTelemetryEventRepository telemetryEventRepository;
    private final DeviceRepository deviceRepository;
    private final PatientAccessService patientAccessService;
    private final EncryptionService encryptionService;
    private final WifiConfigurationCommandPublisher wifiCommandPublisher;
    private final WifiCommandStateService wifiCommandStateService;
    private final DeviceProvisioningLifecycleService lifecycleService;
    private final AfterCommitExecutor afterCommitExecutor;
    private final ObjectMapper objectMapper;

    public DeviceConfigurationServiceImpl(
            DeviceConfigurationRepository configRepository,
            DeviceCommandRepository commandRepository,
            DeviceCommandAcknowledgementRepository acknowledgementRepository,
            DeviceTelemetryEventRepository telemetryEventRepository,
            DeviceRepository deviceRepository,
            PatientAccessService patientAccessService,
            EncryptionService encryptionService,
            WifiConfigurationCommandPublisher wifiCommandPublisher,
            WifiCommandStateService wifiCommandStateService,
            DeviceProvisioningLifecycleService lifecycleService,
            AfterCommitExecutor afterCommitExecutor,
            ObjectMapper objectMapper) {
        this.configRepository = configRepository;
        this.commandRepository = commandRepository;
        this.acknowledgementRepository = acknowledgementRepository;
        this.telemetryEventRepository = telemetryEventRepository;
        this.deviceRepository = deviceRepository;
        this.patientAccessService = patientAccessService;
        this.encryptionService = encryptionService;
        this.wifiCommandPublisher = wifiCommandPublisher;
        this.wifiCommandStateService = wifiCommandStateService;
        this.lifecycleService = lifecycleService;
        this.afterCommitExecutor = afterCommitExecutor;
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
        prepareProvisioningAttempt(config, false);

        config = configRepository.save(config);

        publishConfigCommand(device, config);

        return DeviceConfigurationMapper.toResponseDTO(config);
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceConfigurationResponseDTO getConfigurationStatus(Long outerDeviceId) {
        Device device = getValidatedOuterDevice(outerDeviceId);

        DeviceConfiguration config = configRepository.findByOuterDeviceId(outerDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration not found for this device"));

        DeviceCommand command = findCurrentProvisioningCommand(config);
        DeviceCommandAcknowledgement acknowledgement = command == null
                ? null
                : acknowledgementRepository.findTopByCommandIdOrderByAcknowledgedAtDesc(command.getCommandId()).orElse(null);
        DeviceTelemetryEvent latestInnerResult = telemetryEventRepository
                .findTopByDeviceConfigurationIdAndEventTypeOrderByReceivedAtDesc(
                        config.getConfigurationId(),
                        EVENT_TYPE_INNER_WIFI_CONFIGURATION_RESULT
                )
                .orElse(null);

        return lifecycleService.toStatusResponse(config, device, command, acknowledgement, latestInnerResult);
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

        boolean publishRequired = false;

        if (dto.getWifiSsid() != null && !dto.getWifiSsid().isBlank()) {
            config.setWifiSsid(dto.getWifiSsid());
            publishRequired = true;
        }

        if (dto.getWifiPassword() != null && !dto.getWifiPassword().isBlank()) {
            setEncryptedWifiPassword(config, dto.getWifiPassword());
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
            rememberPreviousSuccessfulConfiguration(config);
            config.setConfigurationVersion(config.getConfigurationVersion() + 1);
            prepareProvisioningAttempt(config, true);
            config = configRepository.save(config);
            publishConfigCommand(device, config);
        }

        return DeviceConfigurationMapper.toResponseDTO(config);
    }

    @Override
    @Transactional
    public DeviceConfigurationResponseDTO sendConfiguration(Long outerDeviceId) {
        Device device = getValidatedOuterDevice(outerDeviceId);

        DeviceConfiguration config = configRepository.findByOuterDeviceId(outerDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration not found for this device"));

        prepareProvisioningAttempt(config, true);
        config = configRepository.save(config);

        publishConfigCommand(device, config);

        return DeviceConfigurationMapper.toResponseDTO(config);
    }

    @Transactional
    public void sendConfigurationForDeviceSync(Device device) {
        if (device == null || device.getDeviceId() == null) {
            return;
        }

        configRepository.findByOuterDeviceId(device.getDeviceId()).ifPresent(config -> {
            prepareProvisioningAttempt(config, true);
            DeviceConfiguration savedConfig = configRepository.save(config);
            publishConfigCommand(device, savedConfig);
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

    private void publishConfigCommand(Device device, DeviceConfiguration config) {
        if (config.getConfigurationVersion() != null && config.getConfigurationVersion() > 1) {
            wifiCommandStateService.expireSupersededWifiCommands(
                    config.getConfigurationId(),
                    config.getConfigurationVersion()
            );
        }

        DeviceCommand command = new DeviceCommand();
        command.setDeviceId(device.getDeviceId());
        command.setPatientId(device.getPatientId());
        command.setDeviceConfigurationId(config.getConfigurationId());
        command.setConfigurationVersion(config.getConfigurationVersion());
        command.setCommandType(COMMAND_TYPE_WIFI_CONFIGURATION);
        command.setCommandStatus("PENDING");
        command.setPayload("{}");
        command = commandRepository.save(command);

        command.setCommandUid("CMD-" + command.getCommandId());
        config.setLastProvisioningCommandId(command.getCommandId());
        config.setLastProvisioningCommandUid(command.getCommandUid());
        configRepository.save(config);

        try {
            command.setPayload(buildSafeWifiCommandMetadata(config));
            command = commandRepository.save(command);

            Long commandId = command.getCommandId();
            afterCommitExecutor.runAfterCommit(() -> {
                try {
                    wifiCommandPublisher.publishWifiCommand(commandId);
                } catch (RuntimeException ignored) {
                    // Publication failures are stored on the command for retry/recovery.
                }
            });
        } catch (JsonProcessingException e) {
            command.setCommandStatus("FAILED");
            command.setLastError("Failed to serialize command payload");
            commandRepository.save(command);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PAYLOAD_SERIALIZATION_ERROR", "Failed to serialize command payload");
        }
    }

    private String buildSafeWifiCommandMetadata(DeviceConfiguration config) throws JsonProcessingException {
        Map<String, Object> safeMetadata = new HashMap<>();
        safeMetadata.put("configurationId", config.getConfigurationId());
        safeMetadata.put("configurationVersion", config.getConfigurationVersion());
        safeMetadata.put("innerDeviceId", config.getInnerDeviceId());
        safeMetadata.put("penDeviceId", config.getPenDeviceId());
        safeMetadata.put("glucometerDeviceId", config.getGlucometerDeviceId());

        return objectMapper.writeValueAsString(safeMetadata);
    }

    private DeviceCommand findCurrentProvisioningCommand(DeviceConfiguration config) {
        if (config == null || config.getConfigurationId() == null) {
            return null;
        }

        if (config.getConfigurationVersion() != null) {
            Optional<DeviceCommand> currentVersionCommand = commandRepository
                    .findTopByDeviceConfigurationIdAndConfigurationVersionAndCommandTypeOrderByCreatedAtDesc(
                            config.getConfigurationId(),
                            config.getConfigurationVersion(),
                            COMMAND_TYPE_WIFI_CONFIGURATION
                    );
            if (currentVersionCommand.isPresent()) {
                return currentVersionCommand.get();
            }
        }

        if (config.getLastProvisioningCommandId() != null) {
            Optional<DeviceCommand> lastRecordedCommand = commandRepository.findById(config.getLastProvisioningCommandId());
            if (lastRecordedCommand.isPresent()) {
                return lastRecordedCommand.get();
            }
        }

        return commandRepository.findTopByDeviceConfigurationIdAndCommandTypeOrderByCreatedAtDesc(
                        config.getConfigurationId(),
                        COMMAND_TYPE_WIFI_CONFIGURATION
                )
                .orElse(null);
    }

    private void prepareProvisioningAttempt(DeviceConfiguration config, boolean preserveInnerAsWaiting) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        config.setConfigurationStatus("PENDING");
        config.setOuterUnitStatus("PENDING");
        config.setMqttStatus("PENDING");
        config.setRollbackStatus("NOT_REQUIRED");
        config.setProvisioningStartedAt(now);
        config.setProvisioningCompletedAt(null);
        config.setProvisioningTimeoutAt(null);
        config.setProvisioningFailureCode(null);
        config.setProvisioningFailureMessage(null);
        config.setInnerUnitIpAddress(null);
        config.setInnerUnitMessage(null);
        if (preserveInnerAsWaiting) {
            config.setInnerUnitStatus("WAITING_FOR_CONFIGURATION");
        } else {
            config.setInnerUnitStatus("NOT_CONFIGURED");
        }
    }

    private void rememberPreviousSuccessfulConfiguration(DeviceConfiguration config) {
        if (config.getLastSuccessfulConfigurationVersion() != null) {
            config.setPreviousConfigurationId(config.getLastSuccessfulConfigurationId());
            config.setPreviousConfigurationVersion(config.getLastSuccessfulConfigurationVersion());
            return;
        }

        if ("APPLIED".equals(config.getConfigurationStatus())) {
            config.setPreviousConfigurationId(config.getConfigurationId());
            config.setPreviousConfigurationVersion(config.getConfigurationVersion());
            config.setLastSuccessfulConfigurationId(config.getConfigurationId());
            config.setLastSuccessfulConfigurationVersion(config.getConfigurationVersion());
            config.setLastSuccessfulAt(config.getLastSyncedAt() != null ? config.getLastSyncedAt() : OffsetDateTime.now(ZoneOffset.UTC));
        }
    }
}
