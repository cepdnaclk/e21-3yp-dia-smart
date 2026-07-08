package com.diasmart.springapi.devices.service;

import com.diasmart.springapi.devices.dto.AssignDeviceRequestDTO;
import com.diasmart.springapi.devices.dto.DeviceDiagnosticsDTO;
import com.diasmart.springapi.devices.dto.DeviceResponseDTO;
import com.diasmart.springapi.devices.dto.DeviceSummaryDTO;
import com.diasmart.springapi.devices.dto.RegisterDeviceRequestDTO;
import com.diasmart.springapi.devices.dto.DeviceKitRegistrationRequestDTO;

import java.util.List;

public interface DeviceService {

    List<DeviceSummaryDTO> getAllDevices();

    DeviceResponseDTO getDeviceById(Long id);

    DeviceResponseDTO registerDevice(RegisterDeviceRequestDTO dto);

    DeviceResponseDTO assignDevice(Long id, AssignDeviceRequestDTO dto);

    void activateDeviceKit(Long patientId, com.diasmart.springapi.devices.dto.PatientDeviceActivationRequestDTO dto);

    DeviceResponseDTO unassignDevice(Long id);

    DeviceDiagnosticsDTO getDeviceDiagnostics(Long id);

    void registerDeviceKit(DeviceKitRegistrationRequestDTO dto);
}
