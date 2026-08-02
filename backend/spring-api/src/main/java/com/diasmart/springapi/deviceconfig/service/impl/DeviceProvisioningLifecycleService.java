package com.diasmart.springapi.deviceconfig.service.impl;

import com.diasmart.springapi.deviceconfig.dto.DeviceConfigurationResponseDTO;
import com.diasmart.springapi.deviceconfig.entity.DeviceCommand;
import com.diasmart.springapi.deviceconfig.entity.DeviceCommandAcknowledgement;
import com.diasmart.springapi.deviceconfig.entity.DeviceConfiguration;
import com.diasmart.springapi.deviceconfig.mapper.DeviceConfigurationMapper;
import com.diasmart.springapi.deviceevents.entity.DeviceTelemetryEvent;
import com.diasmart.springapi.devices.entity.Device;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

@Service
public class DeviceProvisioningLifecycleService {

    private static final Set<String> TERMINAL_OVERALL_STATUSES = Set.of(
            "SUCCEEDED",
            "FAILED",
            "ROLLED_BACK",
            "TIMED_OUT",
            "SUPERSEDED",
            "STALE"
    );
    private static final Set<String> STALE_PROCESSING_RESULTS = Set.of(
            "COMMAND_SUPERSEDED",
            "COMMAND_CONFIGURATION_VERSION_MISMATCH",
            "RESULT_CONFIGURATION_VERSION_MISMATCH",
            "STALE_STATUS_TRANSITION"
    );

    public DeviceConfigurationResponseDTO toStatusResponse(
            DeviceConfiguration config,
            Device outerDevice,
            DeviceCommand command,
            DeviceCommandAcknowledgement acknowledgement,
            DeviceTelemetryEvent latestInnerResult
    ) {
        DeviceConfigurationResponseDTO dto = DeviceConfigurationMapper.toResponseDTO(config);
        dto.setOuterDeviceUid(outerDevice == null ? null : outerDevice.getDeviceUid());

        if (command != null) {
            dto.setCommandNumericId(command.getCommandId());
            dto.setCommandId(command.getCommandUid());
            dto.setCommandStatus(command.getCommandStatus());
            dto.setPublishedAt(command.getPublishedAt());
            dto.setLastAcknowledgedAt(command.getAcknowledgedAt());
            dto.setTimeoutAt(command.getTimeoutAt());
            dto.setCompletedAt(firstNonNull(command.getCompletedAt(), config.getProvisioningCompletedAt()));
        } else {
            dto.setCommandNumericId(config.getLastProvisioningCommandId());
            dto.setCommandId(config.getLastProvisioningCommandUid());
            dto.setCompletedAt(config.getProvisioningCompletedAt());
        }

        if (acknowledgement != null) {
            dto.setLastAckStatus(acknowledgement.getAckStatus());
            dto.setLastAckProcessingResult(acknowledgement.getProcessingResult());
        }

        if (latestInnerResult != null) {
            dto.setLastResultProcessingStatus(latestInnerResult.getProcessingStatus());
            dto.setLastResultProcessingResult(latestInnerResult.getProcessingResult());
        }

        boolean staleResultIgnored = isStaleResult(acknowledgement)
                || isStaleResult(latestInnerResult);
        dto.setStaleResultIgnored(staleResultIgnored);

        String overallStatus = calculateOverallStatus(config, command, staleResultIgnored);
        dto.setOverallStatus(overallStatus);
        dto.setTerminal(TERMINAL_OVERALL_STATUSES.contains(overallStatus));
        dto.setLastErrorCode(resolveLastErrorCode(config, command, acknowledgement, overallStatus));
        dto.setLastErrorMessage(resolveLastErrorMessage(config, command));

        return dto;
    }

    public String calculateOverallStatus(DeviceConfiguration config, DeviceCommand command, boolean staleResultIgnored) {
        if (command != null && isSuperseded(command, config)) {
            return "SUPERSEDED";
        }

        if (isTimedOut(config, command)) {
            return "TIMED_OUT";
        }

        if (isSucceeded(config, command)) {
            return "SUCCEEDED";
        }

        if ("ROLLED_BACK".equals(config.getConfigurationStatus())
                || "ROLLED_BACK".equals(config.getRollbackStatus())
                || hasCommandStatus(command, "ROLLED_BACK")) {
            return "ROLLED_BACK";
        }

        if ("FAILED".equals(config.getConfigurationStatus())
                || hasCommandStatus(command, "FAILED")
                || "FAILED".equals(config.getMqttStatus())) {
            return "FAILED";
        }

        if (hasCommandStatus(command, "EXPIRED")) {
            return "SUPERSEDED";
        }

        if (staleResultIgnored && command == null) {
            return "STALE";
        }

        if (command == null) {
            return "SAVED";
        }

        String commandStatus = command.getCommandStatus();
        String innerStatus = config.getInnerUnitStatus();
        String mqttStatus = config.getMqttStatus();
        String configStatus = config.getConfigurationStatus();

        if ("PENDING".equals(commandStatus) || "SENT".equals(commandStatus)) {
            return "PENDING_PUBLICATION";
        }
        if ("PUBLISHED".equals(commandStatus)) {
            return "PUBLISHED";
        }
        if ("RECEIVED".equals(commandStatus)) {
            return "OUTER_RECEIVED";
        }
        if ("VALIDATED".equals(commandStatus)) {
            return "VALIDATING";
        }
        if ("STAGED".equals(commandStatus)
                || "STAGED".equals(innerStatus)
                || "WAITING_FOR_CONFIGURATION".equals(innerStatus)) {
            return "STAGING_INNER";
        }
        if ("RECONNECTING".equals(mqttStatus)) {
            return "RECONNECTING";
        }
        if ("APPLYING".equals(commandStatus)
                || "APPLYING".equals(configStatus)
                || "CONNECTING".equals(innerStatus)) {
            return "APPLYING";
        }
        if ("APPLIED".equals(commandStatus)) {
            return "RECONNECTING";
        }
        if ("PENDING".equals(configStatus)) {
            return "SAVED";
        }

        return configStatus == null ? "SAVED" : configStatus;
    }

    private boolean isSucceeded(DeviceConfiguration config, DeviceCommand command) {
        return "APPLIED".equals(config.getConfigurationStatus())
                && "APPLIED".equals(config.getOuterUnitStatus())
                && "CONNECTED".equals(config.getInnerUnitStatus())
                && "CONNECTED".equals(config.getMqttStatus())
                && (command == null || "APPLIED".equals(command.getCommandStatus()));
    }

    private boolean isTimedOut(DeviceConfiguration config, DeviceCommand command) {
        if ("TIMED_OUT".equals(config.getConfigurationStatus()) || hasCommandStatus(command, "TIMED_OUT")) {
            return true;
        }

        if (command == null || command.getCompletedAt() != null || command.getTimeoutAt() == null) {
            return false;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return !command.getTimeoutAt().isAfter(now);
    }

    private boolean isSuperseded(DeviceCommand command, DeviceConfiguration config) {
        return command.getConfigurationVersion() != null
                && config.getConfigurationVersion() != null
                && !command.getConfigurationVersion().equals(config.getConfigurationVersion());
    }

    private boolean hasCommandStatus(DeviceCommand command, String status) {
        return command != null && status.equals(command.getCommandStatus());
    }

    private boolean isStaleResult(DeviceCommandAcknowledgement acknowledgement) {
        return acknowledgement != null
                && STALE_PROCESSING_RESULTS.contains(acknowledgement.getProcessingResult());
    }

    private boolean isStaleResult(DeviceTelemetryEvent latestInnerResult) {
        return latestInnerResult != null
                && STALE_PROCESSING_RESULTS.contains(latestInnerResult.getProcessingResult());
    }

    private String resolveLastErrorCode(
            DeviceConfiguration config,
            DeviceCommand command,
            DeviceCommandAcknowledgement acknowledgement,
            String overallStatus
    ) {
        if ("TIMED_OUT".equals(overallStatus)) {
            return firstNonBlank(config.getProvisioningFailureCode(), "INNER_RESULT_TIMEOUT");
        }

        String configFailure = firstNonBlank(config.getProvisioningFailureCode(), null);
        if (configFailure != null) {
            return configFailure;
        }

        String commandError = command == null ? null : firstNonBlank(command.getLastError(), null);
        if (commandError != null) {
            return commandError;
        }

        if (acknowledgement != null && !"ACCEPTED".equals(acknowledgement.getProcessingResult())) {
            return acknowledgement.getProcessingResult();
        }

        return null;
    }

    private String resolveLastErrorMessage(DeviceConfiguration config, DeviceCommand command) {
        String message = firstNonBlank(config.getProvisioningFailureMessage(), config.getInnerUnitMessage());
        if (message != null) {
            return message;
        }
        return command == null ? null : command.getLastError();
    }

    private OffsetDateTime firstNonNull(OffsetDateTime first, OffsetDateTime second) {
        return first != null ? first : second;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
