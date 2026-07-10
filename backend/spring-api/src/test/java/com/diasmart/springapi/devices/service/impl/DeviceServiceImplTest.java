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
import com.diasmart.springapi.devices.dto.PatientDeviceActivationRequestDTO;
import com.diasmart.springapi.devices.entity.DeviceStatus;
import com.diasmart.springapi.patients.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;

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

        private PatientDeviceActivationRequestDTO dto;
        private final Long PATIENT_ID = 100L;

        @BeforeEach
        void setUp() {
                dto = new PatientDeviceActivationRequestDTO();
        }

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

        // --- Helper Method to Create Devices ---
        private Device createDevice(String uid, boolean active, Long patientId) {
                Device device = new Device();
                device.setDeviceUid(uid);
                device.setActive(active);
                device.setPatientId(patientId);
                return device;
        }

        // ==========================================
        // VALID CASES
        // ==========================================

        @Test
        void shouldActivateDeviceKitSuccessfully_AllFourDevices() {
                // Arrange
                dto.setOuterGatewayId("OUT-1");
                dto.setInnerUnitId("INN-1");
                dto.setPenUnitId("PEN-1");
                dto.setGlucoseMeterId("GLU-1");

                Device outer = createDevice("OUT-1", true, null);
                Device inner = createDevice("INN-1", true, null);
                Device pen = createDevice("PEN-1", true, null);
                Device gluco = createDevice("GLU-1", true, null);

                when(deviceRepository.findByDeviceUid("OUT-1")).thenReturn(Optional.of(outer));
                when(deviceRepository.findByDeviceUid("INN-1")).thenReturn(Optional.of(inner));
                when(deviceRepository.findByDeviceUid("PEN-1")).thenReturn(Optional.of(pen));
                when(deviceRepository.findByDeviceUid("GLU-1")).thenReturn(Optional.of(gluco));

                // Act
                deviceService.activateDeviceKit(PATIENT_ID, dto);

                // Assert & Verify
                assertEquals(PATIENT_ID, outer.getPatientId());
                assertEquals(DeviceStatus.CONNECTED, outer.getStatus());
                verify(deviceRepository, times(4)).save(any(Device.class));
        }

        // ==========================================
        // BOUNDARY CASES
        // ==========================================

        @Test
        void shouldActivateDeviceKitSuccessfully_SingleDevice() {
                // Arrange - Only one device provided
                dto.setPenUnitId("PEN-1");
                Device pen = createDevice("PEN-1", true, null);

                when(deviceRepository.findByDeviceUid("PEN-1")).thenReturn(Optional.of(pen));

                // Act
                deviceService.activateDeviceKit(PATIENT_ID, dto);

                // Assert & Verify
                assertEquals(PATIENT_ID, pen.getPatientId());
                verify(deviceRepository, times(1)).save(pen);
        }

        @Test
        void shouldThrowException_FirstDeviceInactive() {
                // Arrange - First device (Outer Gateway) is inactive
                dto.setOuterGatewayId("OUT-1");
                dto.setInnerUnitId("INN-1");

                Device outerInactive = createDevice("OUT-1", false, null); // Inactive
                Device innerActive = createDevice("INN-1", true, null); // Active

                when(deviceRepository.findByDeviceUid("OUT-1")).thenReturn(Optional.of(outerInactive));
                when(deviceRepository.findByDeviceUid("INN-1")).thenReturn(Optional.of(innerActive));

                // Act & Assert
                ApiException exception = assertThrows(ApiException.class, () -> 
                        deviceService.activateDeviceKit(PATIENT_ID, dto)
                );

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
                assertEquals("DEVICE_INACTIVE", exception.getErrorCode());
                verify(deviceRepository, never()).save(any(Device.class)); // Ensures it stopped before saving
        }

        @Test
        void shouldThrowException_LastDeviceInactive() {
                // Arrange - Last device (Glucose Meter) is inactive
                dto.setPenUnitId("PEN-1");
                dto.setGlucoseMeterId("GLU-1");

                Device penActive = createDevice("PEN-1", true, null); // Active
                Device glucoInactive = createDevice("GLU-1", false, null); // Inactive

                when(deviceRepository.findByDeviceUid("PEN-1")).thenReturn(Optional.of(penActive));
                when(deviceRepository.findByDeviceUid("GLU-1")).thenReturn(Optional.of(glucoInactive));

                // Act & Assert
                ApiException exception = assertThrows(ApiException.class, () -> 
                        deviceService.activateDeviceKit(PATIENT_ID, dto)
                );

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
                assertEquals("DEVICE_INACTIVE", exception.getErrorCode());
                verify(deviceRepository, never()).save(any(Device.class));
        }

        // ==========================================
        // INVALID CASES
        // ==========================================

        @Test
        void shouldThrowException_OneDeviceInactive() {
                // Arrange - Only one device provided, and it is inactive
                dto.setOuterGatewayId("OUT-1");
                Device outer = createDevice("OUT-1", false, null);

                when(deviceRepository.findByDeviceUid("OUT-1")).thenReturn(Optional.of(outer));

                // Act & Assert
                ApiException exception = assertThrows(ApiException.class, () -> 
                        deviceService.activateDeviceKit(PATIENT_ID, dto)
                );

                assertEquals("DEVICE_INACTIVE", exception.getErrorCode());
                verify(deviceRepository, never()).save(any());
        }

        @Test
        void shouldThrowException_DeviceAlreadyAssignedToDifferentPatient() {
                // Arrange
                dto.setInnerUnitId("INN-1");
                Long differentPatientId = 999L; 
                Device inner = createDevice("INN-1", true, differentPatientId); // Assigned to someone else

                when(deviceRepository.findByDeviceUid("INN-1")).thenReturn(Optional.of(inner));

                // Act & Assert
                ApiException exception = assertThrows(ApiException.class, () -> 
                        deviceService.activateDeviceKit(PATIENT_ID, dto)
                );

                assertEquals(HttpStatus.CONFLICT, exception.getStatus());
                assertEquals("DEVICE_ALREADY_ASSIGNED", exception.getErrorCode());
                verify(deviceRepository, never()).save(any());
        }

        @Test
        void shouldThrowException_EmptyDeviceListProvided() {
                // Arrange - DTO has all null IDs (empty list)
                
                // Act & Assert
                ApiException exception = assertThrows(ApiException.class, () -> 
                        deviceService.activateDeviceKit(PATIENT_ID, dto)
                );

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
                assertEquals("NO_DEVICES_PROVIDED", exception.getErrorCode());
                verify(deviceRepository, never()).save(any());
        }

        @Test
        void shouldThrowException_BlankDeviceIdsProvided() {
                // Arrange - DTO has empty strings instead of null
                dto.setOuterGatewayId("");
                dto.setInnerUnitId("   ");

                // Act & Assert
                ApiException exception = assertThrows(ApiException.class, () -> 
                        deviceService.activateDeviceKit(PATIENT_ID, dto)
                );

                assertEquals("NO_DEVICES_PROVIDED", exception.getErrorCode());
        }

        @Test
        void shouldActivateSuccessfully_WhenDeviceAssignedToSamePatient() {
                // Arrange - Device is already assigned to THIS patient (idempotency)
                dto.setGlucoseMeterId("GLU-1");
                Device gluco = createDevice("GLU-1", true, PATIENT_ID); // Assigned to SAME patient

                when(deviceRepository.findByDeviceUid("GLU-1")).thenReturn(Optional.of(gluco));

                // Act
                deviceService.activateDeviceKit(PATIENT_ID, dto);

                // Assert
                assertEquals(PATIENT_ID, gluco.getPatientId());
                verify(deviceRepository, times(1)).save(gluco);
        }

        // ==========================================
        // NEGATIVE CASES
        // ==========================================

        @Test
        void shouldThrowException_DeviceMissingInRepository() {
                // Arrange - Repository returns empty
                dto.setPenUnitId("PEN-1");
                
                when(deviceRepository.findByDeviceUid("PEN-1")).thenReturn(Optional.empty());

                // Act & Assert
                ApiException exception = assertThrows(ApiException.class, () -> 
                        deviceService.activateDeviceKit(PATIENT_ID, dto)
                );

                assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
                assertEquals("DEVICE_NOT_FOUND", exception.getErrorCode());
                verify(deviceRepository, never()).save(any());
        }

        @Test
        void shouldThrowException_RepositoryThrowsException() {
                // Arrange - DB Error occurs
                dto.setOuterGatewayId("OUT-1");
                
                when(deviceRepository.findByDeviceUid("OUT-1")).thenThrow(new RuntimeException("DB Connection Lost"));

                // Act & Assert
                RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                        deviceService.activateDeviceKit(PATIENT_ID, dto)
                );

                assertEquals("DB Connection Lost", exception.getMessage());
                verify(deviceRepository, never()).save(any());
        }
}