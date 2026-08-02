package com.diasmart.springapi.devices.service.impl;

import com.diasmart.springapi.audit.service.AuditService;
import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.devices.dto.AssignDeviceRequestDTO;
import com.diasmart.springapi.devices.dto.BuyerDeviceKitsDTO;
import com.diasmart.springapi.devices.dto.DeviceKitDTO;
import com.diasmart.springapi.devices.dto.DeviceKitRegistrationRequestDTO;
import com.diasmart.springapi.devices.dto.DeviceSummaryDTO;
import com.diasmart.springapi.devices.dto.PatientDeviceSummaryDTO;
import com.diasmart.springapi.devices.dto.RegisterDeviceRequestDTO;
import com.diasmart.springapi.devices.entity.Buyer;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.entity.DeviceKit;
import com.diasmart.springapi.devices.entity.DeviceKitDevice;
import com.diasmart.springapi.devices.repository.DeviceHealthLogRepository;
import com.diasmart.springapi.devices.repository.DeviceKitDeviceRepository;
import com.diasmart.springapi.devices.repository.DeviceKitRepository;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.devices.repository.BuyerRepository;
import com.diasmart.springapi.raw_events.repository.RawDeviceEventRepository;
import com.diasmart.springapi.devices.dto.PatientDeviceActivationRequestDTO;
import com.diasmart.springapi.devices.entity.DeviceStatus;
import com.diasmart.springapi.patients.repository.PatientRepository;
import com.diasmart.springapi.relationships.service.PatientAccessService;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

        @Mock
        private DeviceKitRepository deviceKitRepository;

        @Mock
        private DeviceKitDeviceRepository deviceKitDeviceRepository;

        @Mock
        private PatientAccessService patientAccessService;

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
        void getPatientDevicesShouldRequireAccessAndHideBuyerDetails() throws Exception {
                Device device = new Device();
                device.setDeviceId(1L);
                device.setPatientId(PATIENT_ID);
                device.setBuyerId(50L);
                device.setDeviceUid("DEV-001");
                device.setDeviceType("INNER_UNIT");
                device.setDeviceName("Inner Unit");
                device.setFirmwareVersion("1.0.0");
                device.setHardwareVersion("A1");
                device.setActive(true);

                when(deviceRepository.findByPatientIdOrderByDeviceIdAsc(PATIENT_ID))
                                .thenReturn(List.of(device));
                when(healthLogRepository.findTopByDeviceIdOrderByMeasuredAtDesc(1L))
                                .thenReturn(Optional.empty());
                when(rawDeviceEventRepository.findLatestForDeviceDiagnostics(1L))
                                .thenReturn(Optional.empty());

                List<PatientDeviceSummaryDTO> response = deviceService.getPatientDevices(PATIENT_ID);

                assertEquals(1, response.size());
                assertEquals("DEV-001", response.get(0).getDeviceUid());
                assertEquals("INNER_UNIT", response.get(0).getDeviceType());
                assertThrows(
                                NoSuchMethodException.class,
                                () -> PatientDeviceSummaryDTO.class.getMethod("getBuyer"));

                verify(patientAccessService).requireCanViewPatient(PATIENT_ID);
                verify(deviceRepository).findByPatientIdOrderByDeviceIdAsc(PATIENT_ID);
                verify(buyerRepository, never()).findById(anyLong());
        }

        @Test
        void getPatientDevicesShouldNotReadDevicesWhenAccessDenied() {
                doThrow(new AccessDeniedException("denied"))
                                .when(patientAccessService)
                                .requireCanViewPatient(999L);

                assertThrows(
                                AccessDeniedException.class,
                                () -> deviceService.getPatientDevices(999L));

                verify(deviceRepository, never()).findByPatientIdOrderByDeviceIdAsc(anyLong());
        }

        @Test
        void registerDeviceKitShouldCreateBuyerKitFourDevicesAndMemberships() {
                DeviceKitRegistrationRequestDTO request = createKitRegistrationRequest("1");
                Buyer buyer = createBuyer();

                when(deviceRepository.existsByDeviceUid(anyString()))
                                .thenReturn(false);
                when(deviceKitRepository.existsByKitUid("KIT-1"))
                                .thenReturn(false);
                when(buyerRepository.findByNic("991234567V"))
                                .thenReturn(Optional.of(buyer));
                when(buyerRepository.findById(50L))
                                .thenReturn(Optional.of(buyer));
                when(deviceKitRepository.save(any(DeviceKit.class)))
                                .thenAnswer(invocation -> {
                                        DeviceKit kit = invocation.getArgument(0);
                                        kit.setDeviceKitId(77L);
                                        return kit;
                                });
                when(deviceRepository.save(any(Device.class)))
                                .thenAnswer(invocation -> {
                                        Device device = invocation.getArgument(0);
                                        device.setDeviceId(nextDeviceId(device.getDeviceUid()));
                                        return device;
                                });
                when(deviceKitDeviceRepository.existsByDeviceKitIdAndKitDeviceRole(anyLong(), anyString()))
                                .thenReturn(false);
                when(deviceKitDeviceRepository.existsByDeviceId(anyLong()))
                                .thenReturn(false);
                when(deviceKitDeviceRepository.save(any(DeviceKitDevice.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                when(healthLogRepository.findTopByDeviceIdOrderByMeasuredAtDesc(anyLong()))
                                .thenReturn(Optional.empty());
                when(rawDeviceEventRepository.findLatestForDeviceDiagnostics(anyLong()))
                                .thenReturn(Optional.empty());

                DeviceKitDTO response = deviceService.registerDeviceKit(request);

                assertEquals(77L, response.getDeviceKitId());
                assertEquals("KIT-1", response.getKitUid());
                assertEquals(50L, response.getBuyerId());
                assertEquals(LocalDate.of(2026, 8, 1), response.getPurchaseDate());
                assertEquals("ACTIVE", response.getStatus());
                assertEquals(4, response.getDevices().size());
                assertEquals(
                                List.of("OUTER_GATEWAY", "INNER_UNIT", "DOSE_CAP", "GLUCOMETER"),
                                response.getDevices().stream()
                                                .map(DeviceSummaryDTO::getKitDeviceRole)
                                                .toList());

                ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
                verify(deviceRepository, times(4)).save(deviceCaptor.capture());
                assertEquals(
                                List.of("OUTER_GATEWAY", "INNER_UNIT", "DOSE_CAP", "GLUCOMETER"),
                                deviceCaptor.getAllValues().stream()
                                                .map(Device::getDeviceType)
                                                .toList());
                verify(deviceKitDeviceRepository, times(4)).save(any(DeviceKitDevice.class));
                verify(auditService, times(4)).logDeviceRegistration(any(Device.class));
        }

        @Test
        void registerDeviceKitShouldRejectDuplicateDeviceUidInRequest() {
                DeviceKitRegistrationRequestDTO request = createKitRegistrationRequest("1");
                request.setInnerUnitId("OUT-1");

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.registerDeviceKit(request));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
                assertEquals("DUPLICATE_DEVICE_UID_IN_KIT", exception.getErrorCode());
                verify(deviceKitRepository, never()).save(any(DeviceKit.class));
                verify(deviceRepository, never()).save(any(Device.class));
        }

        @Test
        void registerDeviceKitShouldRejectExistingDeviceUid() {
                DeviceKitRegistrationRequestDTO request = createKitRegistrationRequest("1");

                when(deviceRepository.existsByDeviceUid("OUT-1"))
                                .thenReturn(true);

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.registerDeviceKit(request));

                assertEquals(HttpStatus.CONFLICT, exception.getStatus());
                assertEquals("DEVICE_ALREADY_EXISTS", exception.getErrorCode());
                verify(deviceKitRepository, never()).save(any(DeviceKit.class));
        }

        @Test
        void addDeviceToKitShouldRejectDuplicateRole() {
                DeviceKit kit = createKit();
                Device device = createKitDevice(1L, "OUTER_GATEWAY");

                when(deviceKitDeviceRepository.existsByDeviceKitIdAndKitDeviceRole(77L, "OUTER_GATEWAY"))
                                .thenReturn(true);

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.addDeviceToKit(kit, device, "OUTER_GATEWAY"));

                assertEquals(HttpStatus.CONFLICT, exception.getStatus());
                assertEquals("DEVICE_KIT_ROLE_ALREADY_EXISTS", exception.getErrorCode());
                verify(deviceKitDeviceRepository, never()).save(any(DeviceKitDevice.class));
        }

        @Test
        void addDeviceToKitShouldRejectWrongDeviceTypeForRole() {
                DeviceKit kit = createKit();
                Device device = createKitDevice(1L, "INNER_UNIT");

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.addDeviceToKit(kit, device, "OUTER_GATEWAY"));

                assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
                assertEquals("KIT_DEVICE_ROLE_MISMATCH", exception.getErrorCode());
                verify(deviceKitDeviceRepository, never()).save(any(DeviceKitDevice.class));
        }

        @Test
        void addDeviceToKitShouldRejectDeviceAlreadyInAnotherKit() {
                DeviceKit kit = createKit();
                Device device = createKitDevice(1L, "OUTER_GATEWAY");

                when(deviceKitDeviceRepository.existsByDeviceKitIdAndKitDeviceRole(77L, "OUTER_GATEWAY"))
                                .thenReturn(false);
                when(deviceKitDeviceRepository.existsByDeviceId(1L))
                                .thenReturn(true);

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.addDeviceToKit(kit, device, "OUTER_GATEWAY"));

                assertEquals(HttpStatus.CONFLICT, exception.getStatus());
                assertEquals("DEVICE_ALREADY_IN_KIT", exception.getErrorCode());
                verify(deviceKitDeviceRepository, never()).save(any(DeviceKitDevice.class));
        }

        @Test
        void registerDeviceKitShouldAllowSameBuyerToOwnMultipleKits() {
                Buyer buyer = createBuyer();

                when(deviceRepository.existsByDeviceUid(anyString()))
                                .thenReturn(false);
                when(deviceKitRepository.existsByKitUid(anyString()))
                                .thenReturn(false);
                when(buyerRepository.findByNic("991234567V"))
                                .thenReturn(Optional.of(buyer));
                when(buyerRepository.findById(50L))
                                .thenReturn(Optional.of(buyer));
                when(deviceKitRepository.save(any(DeviceKit.class)))
                                .thenAnswer(invocation -> {
                                        DeviceKit kit = invocation.getArgument(0);
                                        kit.setDeviceKitId("KIT-1".equals(kit.getKitUid()) ? 77L : 78L);
                                        return kit;
                                });
                when(deviceRepository.save(any(Device.class)))
                                .thenAnswer(invocation -> {
                                        Device device = invocation.getArgument(0);
                                        device.setDeviceId(nextDeviceId(device.getDeviceUid()));
                                        return device;
                                });
                when(deviceKitDeviceRepository.existsByDeviceKitIdAndKitDeviceRole(anyLong(), anyString()))
                                .thenReturn(false);
                when(deviceKitDeviceRepository.existsByDeviceId(anyLong()))
                                .thenReturn(false);
                when(deviceKitDeviceRepository.save(any(DeviceKitDevice.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                when(healthLogRepository.findTopByDeviceIdOrderByMeasuredAtDesc(anyLong()))
                                .thenReturn(Optional.empty());
                when(rawDeviceEventRepository.findLatestForDeviceDiagnostics(anyLong()))
                                .thenReturn(Optional.empty());

                deviceService.registerDeviceKit(createKitRegistrationRequest("1"));
                deviceService.registerDeviceKit(createKitRegistrationRequest("2"));

                verify(deviceKitRepository, times(2)).save(any(DeviceKit.class));
                verify(buyerRepository, never()).save(any(Buyer.class));
                verify(deviceRepository, times(8)).save(any(Device.class));
        }

        @Test
        void registerDeviceKitShouldBeTransactionalForRollback() throws Exception {
                Transactional transactional = DeviceServiceImpl.class
                                .getMethod("registerDeviceKit", DeviceKitRegistrationRequestDTO.class)
                                .getAnnotation(Transactional.class);

                assertNotNull(transactional);
        }

        @Test
        void registerDeviceKitShouldStopWhenMembershipSaveFails() {
                DeviceKitRegistrationRequestDTO request = createKitRegistrationRequest("1");
                Buyer buyer = createBuyer();

                when(deviceRepository.existsByDeviceUid(anyString()))
                                .thenReturn(false);
                when(deviceKitRepository.existsByKitUid("KIT-1"))
                                .thenReturn(false);
                when(buyerRepository.findByNic("991234567V"))
                                .thenReturn(Optional.of(buyer));
                when(deviceKitRepository.save(any(DeviceKit.class)))
                                .thenAnswer(invocation -> {
                                        DeviceKit kit = invocation.getArgument(0);
                                        kit.setDeviceKitId(77L);
                                        return kit;
                                });
                when(deviceRepository.save(any(Device.class)))
                                .thenAnswer(invocation -> {
                                        Device device = invocation.getArgument(0);
                                        device.setDeviceId(1L);
                                        return device;
                                });
                when(deviceKitDeviceRepository.existsByDeviceKitIdAndKitDeviceRole(anyLong(), anyString()))
                                .thenReturn(false);
                when(deviceKitDeviceRepository.existsByDeviceId(anyLong()))
                                .thenReturn(false);
                when(deviceKitDeviceRepository.save(any(DeviceKitDevice.class)))
                                .thenThrow(new RuntimeException("membership failed"));

                RuntimeException exception = assertThrows(
                                RuntimeException.class,
                                () -> deviceService.registerDeviceKit(request));

                assertEquals("membership failed", exception.getMessage());
                verify(deviceRepository, times(1)).save(any(Device.class));
        }

        @Test
        void getDeviceKitsShouldNotTreatLegacyBuyerDevicesAsKits() {
                when(deviceKitRepository.findAllByOrderByCreatedAtDesc())
                                .thenReturn(List.of());

                List<BuyerDeviceKitsDTO> response = deviceService.getDeviceKits();

                assertTrue(response.isEmpty());
                verify(deviceRepository, never()).findAllByOrderByDeviceIdAsc();
                verify(buyerRepository, never()).findAll();
        }

        @Test
        void getDeviceKitsShouldReturnPersistedKitMemberships() {
                Buyer buyer = createBuyer();
                DeviceKit kit = createKit();
                Device outer = createKitDevice(1L, "OUTER_GATEWAY");
                outer.setBuyerId(50L);
                DeviceKitDevice membership = new DeviceKitDevice();
                membership.setDeviceKitId(77L);
                membership.setDeviceId(1L);
                membership.setKitDeviceRole("OUTER_GATEWAY");

                when(deviceKitRepository.findAllByOrderByCreatedAtDesc())
                                .thenReturn(List.of(kit));
                when(buyerRepository.findAllById(any()))
                                .thenReturn(List.of(buyer));
                when(deviceKitDeviceRepository.findByDeviceKitIdIn(any()))
                                .thenReturn(List.of(membership));
                when(deviceRepository.findAllById(any()))
                                .thenReturn(List.of(outer));
                when(buyerRepository.findById(50L))
                                .thenReturn(Optional.of(buyer));
                when(healthLogRepository.findTopByDeviceIdOrderByMeasuredAtDesc(1L))
                                .thenReturn(Optional.empty());
                when(rawDeviceEventRepository.findLatestForDeviceDiagnostics(1L))
                                .thenReturn(Optional.empty());

                List<BuyerDeviceKitsDTO> response = deviceService.getDeviceKits();

                assertEquals(1, response.size());
                assertEquals(1, response.get(0).getPurchaseCount());
                assertEquals("KIT-1", response.get(0).getKits().get(0).getKitUid());
                assertEquals(
                                "OUTER_GATEWAY",
                                response.get(0).getKits().get(0).getDevices().get(0).getKitDeviceRole());
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

        private DeviceKitRegistrationRequestDTO createKitRegistrationRequest(String suffix) {
                DeviceKitRegistrationRequestDTO request = new DeviceKitRegistrationRequestDTO();
                request.setKitUid("KIT-" + suffix);
                request.setBuyerFullName("John Silva");
                request.setNic("991234567V");
                request.setContactNumber("0771234567");
                request.setAddress("Colombo");
                request.setPurchaseDate(LocalDate.of(2026, 8, 1));
                request.setOuterGatewayId("OUT-" + suffix);
                request.setInnerUnitId("INN-" + suffix);
                request.setPenUnitId("PEN-" + suffix);
                request.setGlucoseMeterId("GLU-" + suffix);
                return request;
        }

        private Buyer createBuyer() {
                Buyer buyer = new Buyer();
                buyer.setBuyerId(50L);
                buyer.setFullName("John Silva");
                buyer.setNic("991234567V");
                buyer.setContactNumber("0771234567");
                buyer.setAddress("Colombo");
                buyer.setPurchaseDate(LocalDate.of(2026, 8, 1));
                return buyer;
        }

        private DeviceKit createKit() {
                DeviceKit kit = new DeviceKit();
                kit.setDeviceKitId(77L);
                kit.setKitUid("KIT-1");
                kit.setBuyerId(50L);
                kit.setPurchaseDate(LocalDate.of(2026, 8, 1));
                kit.setStatus("ACTIVE");
                return kit;
        }

        private Device createKitDevice(Long id, String deviceType) {
                Device device = new Device();
                device.setDeviceId(id);
                device.setDeviceUid(deviceType + "-" + id);
                device.setDeviceType(deviceType);
                device.setDeviceName(deviceType);
                device.setActive(true);
                return device;
        }

        private Long nextDeviceId(String deviceUid) {
                return Integer.toUnsignedLong(deviceUid.hashCode()) + 1L;
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
