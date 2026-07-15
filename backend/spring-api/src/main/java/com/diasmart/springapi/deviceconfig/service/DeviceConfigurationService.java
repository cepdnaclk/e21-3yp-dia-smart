package com.diasmart.springapi.deviceconfig.service;

import com.diasmart.springapi.deviceconfig.dto.CreateDeviceConfigurationRequestDTO;
import com.diasmart.springapi.deviceconfig.dto.DeviceConfigurationResponseDTO;
import com.diasmart.springapi.deviceconfig.dto.UpdateDeviceConfigurationRequestDTO;

public interface DeviceConfigurationService {

    DeviceConfigurationResponseDTO createConfiguration(CreateDeviceConfigurationRequestDTO dto);

    DeviceConfigurationResponseDTO getConfiguration(Long outerDeviceId);

    DeviceConfigurationResponseDTO updateConfiguration(Long outerDeviceId, UpdateDeviceConfigurationRequestDTO dto);
}
