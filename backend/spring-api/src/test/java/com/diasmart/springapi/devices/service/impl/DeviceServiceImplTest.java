package com.diasmart.springapi.devices.service.impl;

import com.diasmart.springapi.audit.service.AuditService;
import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.devices.dto.AssignDeviceRequestDTO;
import com.diasmart.springapi.devices.dto.RegisterDeviceRequestDTO;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.repository.DeviceHealthLogRepository;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.raw_events.repository.RawDeviceEventRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceHealthLogRepository healthLogRepository;

    @Mock
    private RawDeviceEventRepository rawDeviceEventRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private DeviceServiceImpl deviceService;

    @Test
    void getDeviceByIdShouldReturnDevice() {

        Device device = new Device();
        device.setDeviceId(1L);
        device.setDeviceUid("DEV-001");
        device.setDeviceType("INNER_UNIT");
        device.setActive(true);

        when(deviceRepository.findById(1L))
                .thenReturn(Optional.of(device));

        assertNotNull(
                deviceService.getDeviceById(1L)
        );
    }

    @Test
    void getDeviceByIdShouldThrowWhenMissing() {

        when(deviceRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ApiException.class,
                () -> deviceService.getDeviceById(1L)
        );
    }

    @Test
    void getAllDevicesShouldReturnList() {

        Device device = new Device();
        device.setDeviceId(1L);
        device.setDeviceUid("DEV-001");
        device.setDeviceType("INNER_UNIT");
        device.setActive(true);

        when(deviceRepository.findAllByOrderByDeviceIdAsc())
                .thenReturn(List.of(device));

        assertEquals(
                1,
                deviceService.getAllDevices().size()
        );
    }

    @Test
    void assignDeviceShouldUpdatePatientId() {

        Device device = new Device();
        device.setDeviceId(1L);

        AssignDeviceRequestDTO dto =
                new AssignDeviceRequestDTO();

        dto.setPatientId(100L);

        when(deviceRepository.findById(1L))
                .thenReturn(Optional.of(device));

        when(deviceRepository.save(any(Device.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        deviceService.assignDevice(1L, dto);

        assertEquals(
                100L,
                device.getPatientId()
        );

        verify(auditService)
                .logDeviceAssignment(
                        any(),
                        any(),
                        eq(100L)
                );
    }

    @Test
    void registerDeviceShouldCreateDevice() {

        RegisterDeviceRequestDTO dto =
                new RegisterDeviceRequestDTO();

        dto.setDeviceUid("DEV-001");
        dto.setDeviceType("INNER_UNIT");

        when(deviceRepository.findByDeviceUid("DEV-001"))
                .thenReturn(Optional.empty());

        when(deviceRepository.save(any(Device.class)))
                .thenAnswer(inv -> {
                    Device d = inv.getArgument(0);
                    d.setDeviceId(1L);
                    return d;
                });

        assertNotNull(
                deviceService.registerDevice(dto)
        );

        verify(auditService)
                .logDeviceRegistration(any());
    }
}