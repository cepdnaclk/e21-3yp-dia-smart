package com.diasmart.springapi.devices.service.impl;

import com.diasmart.springapi.audit.service.AuditService;
import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.devices.dto.AssignDeviceRequestDTO;
import com.diasmart.springapi.devices.dto.RegisterDeviceRequestDTO;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.repository.DeviceHealthLogRepository;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.devices.repository.BuyerRepository;
import com.diasmart.springapi.raw_events.repository.RawDeviceEventRepository;
import com.diasmart.springapi.patients.repository.PatientRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

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

        @Mock
        private BuyerRepository buyerRepository;

        @Mock
        private PatientRepository patientRepository;

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
                                deviceService.getDeviceById(1L));
        }

        @Test
        void getDeviceByIdShouldThrowWhenMissing() {

                when(deviceRepository.findById(1L))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ApiException.class,
                                () -> deviceService.getDeviceById(1L));
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
                                deviceService.getAllDevices().size());
        }

        @Test
        void assignDeviceShouldUpdatePatientId() {

                Device device = new Device();
                device.setDeviceId(1L);
                device.setActive(true);
                device.setBuyerId(50L);

                AssignDeviceRequestDTO dto = new AssignDeviceRequestDTO();

                dto.setPatientId(100L);

                when(deviceRepository.findById(1L))
                                .thenReturn(Optional.of(device));

                when(buyerRepository.findById(50L))
                                .thenReturn(Optional.empty());

                when(deviceRepository.save(any(Device.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                deviceService.assignDevice(1L, dto);

                assertEquals(
                                100L,
                                device.getPatientId());

                verify(auditService)
                                .logDeviceAssignment(
                                                any(),
                                                any(),
                                                eq(100L));
        }

        @Test
        void assignDeviceShouldRejectWhenAssignedToAnotherPatient() {

                Device device = new Device();
                device.setDeviceId(1L);
                device.setPatientId(77L);
                device.setActive(true);
                device.setBuyerId(50L);

                AssignDeviceRequestDTO dto = new AssignDeviceRequestDTO();
                dto.setPatientId(100L);

                when(deviceRepository.findById(1L))
                                .thenReturn(Optional.of(device));

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.assignDevice(1L, dto));

                assertEquals(HttpStatus.CONFLICT, exception.getStatus());
                assertEquals("DEVICE_ALREADY_ASSIGNED", exception.getErrorCode());
        }

        @Test
        void assignDeviceShouldRejectInactiveDevices() {

                Device device = new Device();
                device.setDeviceId(1L);
                device.setActive(false);
                device.setBuyerId(50L);

                AssignDeviceRequestDTO dto = new AssignDeviceRequestDTO();
                dto.setPatientId(100L);

                when(deviceRepository.findById(1L))
                                .thenReturn(Optional.of(device));

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.assignDevice(1L, dto));

                assertEquals(HttpStatus.CONFLICT, exception.getStatus());
                assertEquals("DEVICE_INACTIVE", exception.getErrorCode());
        }

        @Test
        void registerDeviceShouldCreateDevice() {

                RegisterDeviceRequestDTO dto = new RegisterDeviceRequestDTO();

                dto.setDeviceUid("DEV-001");
                dto.setDeviceType("PEN_UNIT");

                when(deviceRepository.findByDeviceUid("DEV-001"))
                                .thenReturn(Optional.empty());

                when(deviceRepository.save(any(Device.class)))
                                .thenAnswer(inv -> {
                                        Device d = inv.getArgument(0);
                                        d.setDeviceId(1L);
                                        return d;
                                });

                assertNotNull(
                                deviceService.registerDevice(dto));

                verify(auditService)
                                .logDeviceRegistration(any());
        }

        @Test
        void unassignDeviceShouldClearPatientId() {

                Device device = new Device();
                device.setDeviceId(1L);
                device.setPatientId(100L);

                when(deviceRepository.findById(1L))
                                .thenReturn(Optional.of(device));

                when(deviceRepository.save(any(Device.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                deviceService.unassignDevice(1L);

                assertNull(device.getPatientId());

                verify(auditService)
                                .logDeviceUnassignment(eq(device), eq(100L));
        }
}