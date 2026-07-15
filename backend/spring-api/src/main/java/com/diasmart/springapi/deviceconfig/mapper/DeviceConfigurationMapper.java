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
        dto.setConfigurationVersion(entity.getConfigurationVersion());
        dto.setLastSyncedAt(entity.getLastSyncedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        
        return dto;
    }
}
