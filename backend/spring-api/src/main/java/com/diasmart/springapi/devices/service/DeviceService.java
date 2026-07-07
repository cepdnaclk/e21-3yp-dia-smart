package com.diasmart.springapi.devices.service;

import com.diasmart.springapi.devices.dto.AssignDeviceRequestDTO;
import com.diasmart.springapi.devices.dto.DeviceDiagnosticsDTO;
import com.diasmart.springapi.devices.dto.DeviceResponseDTO;
import com.diasmart.springapi.devices.dto.DeviceSummaryDTO;
import com.diasmart.springapi.devices.dto.RegisterDeviceRequestDTO;

import java.util.List;

public interface DeviceService {

    List<DeviceSummaryDTO> getAllDevices();

    DeviceResponseDTO getDeviceById(Long id);

    DeviceResponseDTO registerDevice(RegisterDeviceRequestDTO dto);

    DeviceResponseDTO assignDevice(Long id, AssignDeviceRequestDTO dto);

    DeviceResponseDTO unassignDevice(Long id);

    DeviceDiagnosticsDTO getDeviceDiagnostics(Long id);

}
