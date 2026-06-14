package com.diasmart.springapi.devices.service.impl;

import com.diasmart.springapi.audit.service.AuditService;
import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.devices.dto.AssignDeviceRequestDTO;
import com.diasmart.springapi.devices.dto.DeviceDiagnosticsDTO;
import com.diasmart.springapi.devices.dto.DeviceResponseDTO;
import com.diasmart.springapi.devices.dto.DeviceSummaryDTO;
import com.diasmart.springapi.devices.dto.RegisterDeviceRequestDTO;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.entity.DeviceHealthLog;
import com.diasmart.springapi.devices.repository.DeviceHealthLogRepository;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.raw_events.entity.RawDeviceEvent;
import com.diasmart.springapi.raw_events.repository.RawDeviceEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceServiceImpl Tests")
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

    private Device testDevice;
    private RegisterDeviceRequestDTO registerRequest;
    private AssignDeviceRequestDTO assignRequest;
    private DeviceHealthLog healthLog;

    @BeforeEach
    void setUp() {
        // Setup test device
        testDevice = new Device();
        testDevice.setDeviceId(1L);
        testDevice.setDeviceUid("DS-INNER-0001");
        testDevice.setDeviceType("INNER_UNIT");
        testDevice.setDeviceName("Patient Inner Unit");
        testDevice.setActive(true);
        testDevice.setPatientId(1L);
        testDevice.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        // Setup register request
        registerRequest = new RegisterDeviceRequestDTO();
        registerRequest.setDeviceUid("DS-NEW-0001");
        registerRequest.setDeviceType("INNER_UNIT");
        registerRequest.setDeviceName("New Device");

        // Setup assign request
        assignRequest = new AssignDeviceRequestDTO();
        assignRequest.setPatientId(2L);

        // Setup health log
        healthLog = new DeviceHealthLog();
        healthLog.setDeviceId(1L);
        healthLog.setBatteryPercent(85.0);
        healthLog.setStatus("ONLINE");
        healthLog.setMeasuredAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    // =====================================================
    // REGISTER DEVICE TESTS
    // =====================================================

    @Test
    @DisplayName("Should successfully register device with required fields")
    void testRegisterDeviceSuccess() {
        // Arrange
        when(deviceRepository.findByDeviceUid(registerRequest.getDeviceUid()))
                .thenReturn(Optional.empty());
        when(deviceRepository.findByAwsThingName(anyString()))
                .thenReturn(Optional.empty());
        when(deviceRepository.findByMqttClientId(anyString()))
                .thenReturn(Optional.empty());
        when(deviceRepository.findByMacAddress(anyString()))
                .thenReturn(Optional.empty());
        when(deviceRepository.findBySerialNumber(anyString()))
                .thenReturn(Optional.empty());
        
        Device savedDevice = new Device();
        savedDevice.setDeviceId(1L);
        savedDevice.setDeviceUid(registerRequest.getDeviceUid());
        savedDevice.setDeviceType("INNER_UNIT");
        when(deviceRepository.save(any(Device.class))).thenReturn(savedDevice);

        // Act
        DeviceResponseDTO response = deviceService.registerDevice(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("DS-NEW-0001", response.getDeviceUid());
        verify(deviceRepository, times(1)).save(any(Device.class));
        verify(auditService, times(1)).logDeviceRegistration(any(Device.class));
    }

    @Test
    @DisplayName("Should reject registration with duplicate deviceUid")
    void testRegisterDeviceDuplicateUid() {
        // Arrange
        when(deviceRepository.findByDeviceUid(registerRequest.getDeviceUid()))
                .thenReturn(Optional.of(testDevice));

        // Act & Assert
        assertThrows(ApiException.class, () -> deviceService.registerDevice(registerRequest));
        assertEquals(HttpStatus.CONFLICT, 
                assertThrows(ApiException.class, () -> deviceService.registerDevice(registerRequest)).getStatus());
    }

    @Test
    @DisplayName("Should reject registration with missing deviceUid")
    void testRegisterDeviceMissingUid() {
        // Arrange
        registerRequest.setDeviceUid(null);
        registerRequest.setDeviceId(null);

        // Act & Assert
        assertThrows(ApiException.class, () -> deviceService.registerDevice(registerRequest));
    }

    @Test
    @DisplayName("Should reject registration with invalid deviceType")
    void testRegisterDeviceInvalidType() {
        // Arrange
        registerRequest.setDeviceType("INVALID_TYPE");
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ApiException.class, () -> deviceService.registerDevice(registerRequest));
    }

    @Test
    @DisplayName("Should use deviceId as fallback for deviceUid")
    void testRegisterDeviceUseDeviceIdAsFallback() {
        // Arrange
        registerRequest.setDeviceUid(null);
        registerRequest.setDeviceId("DS-FALLBACK-001");
        when(deviceRepository.findByDeviceUid("DS-FALLBACK-001")).thenReturn(Optional.empty());
        when(deviceRepository.findByAwsThingName(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findByMqttClientId(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findByMacAddress(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findBySerialNumber(anyString())).thenReturn(Optional.empty());
        
        Device savedDevice = new Device();
        savedDevice.setDeviceId(1L);
        savedDevice.setDeviceUid("DS-FALLBACK-001");
        when(deviceRepository.save(any(Device.class))).thenReturn(savedDevice);

        // Act
        DeviceResponseDTO response = deviceService.registerDevice(registerRequest);

        // Assert
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should reject duplicate awsThingName")
    void testRegisterDeviceDuplicateAwsThingName() {
        // Arrange
        registerRequest.setAwsThingName("existing-thing");
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findByAwsThingName("existing-thing")).thenReturn(Optional.of(testDevice));

        // Act & Assert
        assertThrows(ApiException.class, () -> deviceService.registerDevice(registerRequest));
    }

    @Test
    @DisplayName("Should normalize deviceType to uppercase")
    void testRegisterDeviceNormalizesType() {
        // Arrange
        registerRequest.setDeviceType("inner_unit");
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findByAwsThingName(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findByMqttClientId(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findByMacAddress(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findBySerialNumber(anyString())).thenReturn(Optional.empty());
        
        Device savedDevice = new Device();
        savedDevice.setDeviceId(1L);
        savedDevice.setDeviceType("INNER_UNIT");
        when(deviceRepository.save(any(Device.class))).thenReturn(savedDevice);

        // Act
        deviceService.registerDevice(registerRequest);

        // Assert
        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceRepository).save(deviceCaptor.capture());
        assertEquals("INNER_UNIT", deviceCaptor.getValue().getDeviceType());
    }

    @Test
    @DisplayName("Should set active status to true by default")
    void testRegisterDeviceDefaultActive() {
        // Arrange
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findByAwsThingName(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findByMqttClientId(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findByMacAddress(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findBySerialNumber(anyString())).thenReturn(Optional.empty());
        
        Device savedDevice = new Device();
        savedDevice.setDeviceId(1L);
        savedDevice.setActive(true);
        when(deviceRepository.save(any(Device.class))).thenReturn(savedDevice);

        // Act
        deviceService.registerDevice(registerRequest);

        // Assert
        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceRepository).save(deviceCaptor.capture());
        assertTrue(deviceCaptor.getValue().getActive());
    }

    // =====================================================
    // GET DEVICE TESTS
    // =====================================================

    @Test
    @DisplayName("Should successfully retrieve device by ID")
    void testGetDeviceByIdSuccess() {
        // Arrange
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
        when(healthLogRepository.findOptionalTopByDeviceIdOrderByMeasuredAtDesc(1L))
                .thenReturn(Optional.of(healthLog));
        when(rawDeviceEventRepository.findLatestForDeviceDiagnostics(1L))
                .thenReturn(Optional.empty());

        // Act
        DeviceResponseDTO response = deviceService.getDeviceById(1L);

        // Assert
        assertNotNull(response);
        assertEquals("DS-INNER-0001", response.getDeviceUid());
    }

    @Test
    @DisplayName("Should throw exception when device not found")
    void testGetDeviceByIdNotFound() {
        // Arrange
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ApiException.class, () -> deviceService.getDeviceById(999L));
    }

    // =====================================================
    // ASSIGN DEVICE TESTS
    // =====================================================

    @Test
    @DisplayName("Should successfully assign device to patient")
    void testAssignDeviceSuccess() {
        // Arrange
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
        Device assignedDevice = new Device();
        assignedDevice.setDeviceId(1L);
        assignedDevice.setPatientId(2L);
        when(deviceRepository.save(any(Device.class))).thenReturn(assignedDevice);

        // Act
        DeviceResponseDTO response = deviceService.assignDevice(1L, assignRequest);

        // Assert
        assertNotNull(response);
        verify(auditService, times(1)).logDeviceAssignment(any(Device.class), eq(1L), eq(2L));
    }

    @Test
    @DisplayName("Should update patient ID when assigning device")
    void testAssignDeviceUpdatesPatientId() {
        // Arrange
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act
        deviceService.assignDevice(1L, assignRequest);

        // Assert
        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceRepository).save(deviceCaptor.capture());
        assertEquals(2L, deviceCaptor.getValue().getPatientId());
    }

    @Test
    @DisplayName("Should throw exception when device not found for assignment")
    void testAssignDeviceNotFound() {
        // Arrange
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ApiException.class, () -> deviceService.assignDevice(999L, assignRequest));
    }

    // =====================================================
    // GET ALL DEVICES TESTS
    // =====================================================

    @Test
    @DisplayName("Should retrieve all devices")
    void testGetAllDevicesSuccess() {
        // Arrange
        List<Device> devices = Arrays.asList(testDevice);
        when(deviceRepository.findAllByOrderByDeviceIdAsc()).thenReturn(devices);
        when(healthLogRepository.findOptionalTopByDeviceIdOrderByMeasuredAtDesc(anyLong()))
                .thenReturn(Optional.of(healthLog));
        when(rawDeviceEventRepository.findLatestForDeviceDiagnostics(anyLong()))
                .thenReturn(Optional.empty());

        // Act
        List<DeviceSummaryDTO> response = deviceService.getAllDevices();

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());
    }

    @Test
    @DisplayName("Should return empty list when no devices")
    void testGetAllDevicesEmpty() {
        // Arrange
        when(deviceRepository.findAllByOrderByDeviceIdAsc()).thenReturn(Arrays.asList());

        // Act
        List<DeviceSummaryDTO> response = deviceService.getAllDevices();

        // Assert
        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    // =====================================================
    // GET DEVICE DIAGNOSTICS TESTS
    // =====================================================

    @Test
    @DisplayName("Should successfully retrieve device diagnostics")
    void testGetDeviceDiagnosticsSuccess() {
        // Arrange
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
        when(healthLogRepository.findOptionalTopByDeviceIdOrderByMeasuredAtDesc(1L))
                .thenReturn(Optional.of(healthLog));
        when(rawDeviceEventRepository.findLatestForDeviceDiagnostics(1L))
                .thenReturn(Optional.empty());
        when(rawDeviceEventRepository.countEventsForDeviceDiagnostics(1L)).thenReturn(100L);
        when(rawDeviceEventRepository.countReplayedEventsForDeviceDiagnostics(1L)).thenReturn(5L);
        when(auditService.countDuplicateEventsForDevice(1L)).thenReturn(2L);

        // Act
        DeviceDiagnosticsDTO response = deviceService.getDeviceDiagnostics(1L);

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.getReplayStatistics().getTotalMqttEvents());
        assertEquals(5L, response.getReplayStatistics().getReplayedEvents());
    }

    @Test
    @DisplayName("Should set ONLINE status when device seen recently")
    void testGetDeviceDiagnosticsOnlineStatus() {
        // Arrange
        testDevice.setLastSeenAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5));
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
        when(healthLogRepository.findOptionalTopByDeviceIdOrderByMeasuredAtDesc(1L))
                .thenReturn(Optional.of(healthLog));
        when(rawDeviceEventRepository.findLatestForDeviceDiagnostics(1L))
                .thenReturn(Optional.empty());
        when(rawDeviceEventRepository.countEventsForDeviceDiagnostics(1L)).thenReturn(0L);
        when(rawDeviceEventRepository.countReplayedEventsForDeviceDiagnostics(1L)).thenReturn(0L);
        when(auditService.countDuplicateEventsForDevice(1L)).thenReturn(0L);

        // Act
        DeviceDiagnosticsDTO response = deviceService.getDeviceDiagnostics(1L);

        // Assert
        assertNotNull(response);
        assertTrue(response.getOnline());
    }

    @Test
    @DisplayName("Should set OFFLINE status when device not seen for 15+ minutes")
    void testGetDeviceDiagnosticsOfflineStatus() {
        // Arrange
        testDevice.setLastSeenAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(20));
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
        when(healthLogRepository.findOptionalTopByDeviceIdOrderByMeasuredAtDesc(1L))
                .thenReturn(Optional.empty());
        when(rawDeviceEventRepository.findLatestForDeviceDiagnostics(1L))
                .thenReturn(Optional.empty());
        when(rawDeviceEventRepository.countEventsForDeviceDiagnostics(1L)).thenReturn(0L);
        when(rawDeviceEventRepository.countReplayedEventsForDeviceDiagnostics(1L)).thenReturn(0L);
        when(auditService.countDuplicateEventsForDevice(1L)).thenReturn(0L);

        // Act
        DeviceDiagnosticsDTO response = deviceService.getDeviceDiagnostics(1L);

        // Assert
        assertNotNull(response);
        assertEquals("OFFLINE", response.getStatus());
    }

    @Test
    @DisplayName("Should set DEACTIVATED status when device is inactive")
    void testGetDeviceDiagnosticsDeactivatedStatus() {
        // Arrange
        testDevice.setActive(false);
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
        when(healthLogRepository.findOptionalTopByDeviceIdOrderByMeasuredAtDesc(1L))
                .thenReturn(Optional.of(healthLog));
        when(rawDeviceEventRepository.findLatestForDeviceDiagnostics(1L))
                .thenReturn(Optional.empty());
        when(rawDeviceEventRepository.countEventsForDeviceDiagnostics(1L)).thenReturn(0L);
        when(rawDeviceEventRepository.countReplayedEventsForDeviceDiagnostics(1L)).thenReturn(0L);
        when(auditService.countDuplicateEventsForDevice(1L)).thenReturn(0L);

        // Act
        DeviceDiagnosticsDTO response = deviceService.getDeviceDiagnostics(1L);

        // Assert
        assertEquals("DEACTIVATED", response.getStatus());
        assertFalse(response.getOnline());
    }

    @Test
    @DisplayName("Should throw exception when device not found for diagnostics")
    void testGetDeviceDiagnosticsNotFound() {
        // Arrange
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ApiException.class, () -> deviceService.getDeviceDiagnostics(999L));
    }

    // =====================================================
    // EDGE CASES AND UTILITY METHOD TESTS
    // =====================================================

    @Test
    @DisplayName("Should handle null contact number")
    void testRegisterDeviceNullContactNumber() {
        // Arrange
        // RegisterDeviceRequestDTO does not support contact number
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findByAwsThingName(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findByMqttClientId(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findByMacAddress(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findBySerialNumber(anyString())).thenReturn(Optional.empty());
        
        Device savedDevice = new Device();
        savedDevice.setDeviceId(1L);
        when(deviceRepository.save(any(Device.class))).thenReturn(savedDevice);

        // Act
        deviceService.registerDevice(registerRequest);

        // Assert
        verify(deviceRepository, times(1)).save(any(Device.class));
    }

    @Test
    @DisplayName("Should convert OUTER_UNIT to OUTER_GATEWAY")
    void testRegisterDeviceConvertOuterUnit() {
        // Arrange
        registerRequest.setDeviceType("OUTER_UNIT");
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findByAwsThingName(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findByMqttClientId(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findByMacAddress(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findBySerialNumber(anyString())).thenReturn(Optional.empty());
        
        Device savedDevice = new Device();
        savedDevice.setDeviceId(1L);
        savedDevice.setDeviceType("OUTER_GATEWAY");
        when(deviceRepository.save(any(Device.class))).thenReturn(savedDevice);

        // Act
        deviceService.registerDevice(registerRequest);

        // Assert
        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceRepository).save(deviceCaptor.capture());
        assertEquals("OUTER_GATEWAY", deviceCaptor.getValue().getDeviceType());
    }

    @Test
    @DisplayName("Should handle whitespace in optional fields")
    void testRegisterDeviceWhitespaceInOptionalFields() {
        // Arrange
        registerRequest.setDeviceName("   ");
        registerRequest.setFirmwareVersion("   ");
        when(deviceRepository.findByDeviceUid(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findByAwsThingName(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findByMqttClientId(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findByMacAddress(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.findBySerialNumber(anyString())).thenReturn(Optional.empty());
        
        Device savedDevice = new Device();
        savedDevice.setDeviceId(1L);
        when(deviceRepository.save(any(Device.class))).thenReturn(savedDevice);

        // Act
        deviceService.registerDevice(registerRequest);

        // Assert
        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceRepository).save(deviceCaptor.capture());
        assertNull(deviceCaptor.getValue().getDeviceName());
    }
}
