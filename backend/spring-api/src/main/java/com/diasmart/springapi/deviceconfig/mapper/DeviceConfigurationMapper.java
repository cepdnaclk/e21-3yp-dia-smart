package com.diasmart.springapi.deviceconfig.mapper;

import com.diasmart.springapi.deviceconfig.dto.DeviceConfigurationResponseDTO;
import com.diasmart.springapi.deviceconfig.entity.DeviceConfiguration;

public class DeviceConfigurationMapper {

    private DeviceConfigurationMapper() {
    }

    public static DeviceConfigurationResponseDTO toResponseDTO(DeviceConfiguration entity) {
        if (entity == null) {
            return null;
        }

        DeviceConfigurationResponseDTO dto = new DeviceConfigurationResponseDTO();
        dto.setConfigurationId(entity.getConfigurationId());
        dto.setOuterDeviceId(entity.getOuterDeviceId());
        dto.setPatientId(entity.getPatientId());
        dto.setInnerDeviceId(entity.getInnerDeviceId());
        dto.setPenDeviceId(entity.getPenDeviceId());
        dto.setGlucometerDeviceId(entity.getGlucometerDeviceId());
        dto.setWifiSsid(entity.getWifiSsid());
        dto.setConfigurationStatus(entity.getConfigurationStatus());
        dto.setOuterUnitStatus(entity.getOuterUnitStatus());
        dto.setInnerUnitStatus(entity.getInnerUnitStatus());
        dto.setInnerUnitIpAddress(entity.getInnerUnitIpAddress());
        dto.setInnerUnitMessage(entity.getInnerUnitMessage());
        dto.setLastInnerUnitStatusAt(entity.getLastInnerUnitStatusAt());
        dto.setConfigurationVersion(entity.getConfigurationVersion());
        dto.setLastSuccessfulConfigurationId(entity.getLastSuccessfulConfigurationId());
        dto.setLastSuccessfulConfigurationVersion(entity.getLastSuccessfulConfigurationVersion());
        dto.setLastSuccessfulAt(entity.getLastSuccessfulAt());
        dto.setPreviousConfigurationId(entity.getPreviousConfigurationId());
        dto.setPreviousConfigurationVersion(entity.getPreviousConfigurationVersion());
        dto.setProvisioningStartedAt(entity.getProvisioningStartedAt());
        dto.setProvisioningCompletedAt(entity.getProvisioningCompletedAt());
        dto.setProvisioningTimeoutAt(entity.getProvisioningTimeoutAt());
        dto.setProvisioningFailureCode(entity.getProvisioningFailureCode());
        dto.setProvisioningFailureMessage(entity.getProvisioningFailureMessage());
        dto.setRollbackStatus(entity.getRollbackStatus());
        dto.setMqttStatus(entity.getMqttStatus());
        dto.setLastProvisioningCommandId(entity.getLastProvisioningCommandId());
        dto.setLastProvisioningCommandUid(entity.getLastProvisioningCommandUid());
        dto.setLastSyncedAt(entity.getLastSyncedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        
        return dto;
    }
}
