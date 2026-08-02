package com.diasmart.springapi.deviceconfig.service;

import com.diasmart.springapi.deviceconfig.dto.CreateDeviceConfigurationRequestDTO;
import com.diasmart.springapi.deviceconfig.dto.DeviceConfigurationResponseDTO;
import com.diasmart.springapi.deviceconfig.dto.UpdateDeviceConfigurationRequestDTO;

import java.util.List;

public interface DeviceConfigurationService {

    DeviceConfigurationResponseDTO createConfiguration(CreateDeviceConfigurationRequestDTO dto);

    List<DeviceConfigurationResponseDTO> getConfigurations();

    DeviceConfigurationResponseDTO getConfiguration(Long outerDeviceId);

    DeviceConfigurationResponseDTO getConfigurationStatus(Long outerDeviceId);

    DeviceConfigurationResponseDTO updateConfiguration(Long outerDeviceId, UpdateDeviceConfigurationRequestDTO dto);

    DeviceConfigurationResponseDTO sendConfiguration(Long outerDeviceId);
}
