package com.diasmart.springapi.devices.service.impl;

import com.diasmart.springapi.audit.service.AuditService;
import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.devices.dto.AssignDeviceRequestDTO;
import com.diasmart.springapi.devices.dto.BuyerDeviceKitsDTO;
import com.diasmart.springapi.devices.dto.DeviceKitActivationResponseDTO;
import com.diasmart.springapi.devices.dto.DeviceKitDTO;
import com.diasmart.springapi.devices.dto.DeviceKitRegistrationRequestDTO;
import com.diasmart.springapi.devices.dto.DeviceSummaryDTO;
import com.diasmart.springapi.devices.dto.PatientDeviceSummaryDTO;
import com.diasmart.springapi.devices.dto.RegisterDeviceRequestDTO;
import com.diasmart.springapi.devices.entity.Buyer;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.entity.DeviceActivationFailureCategory;
import com.diasmart.springapi.devices.entity.DeviceKit;
import com.diasmart.springapi.devices.entity.DeviceKitDevice;
import com.diasmart.springapi.devices.repository.DeviceHealthLogRepository;
import com.diasmart.springapi.devices.repository.DeviceKitDeviceRepository;
import com.diasmart.springapi.devices.repository.DeviceKitRepository;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.devices.repository.BuyerRepository;
import com.diasmart.springapi.devices.service.DeviceActivationAttemptService;
import com.diasmart.springapi.raw_events.repository.RawDeviceEventRepository;
import com.diasmart.springapi.devices.dto.PatientDeviceActivationRequestDTO;
import com.diasmart.springapi.devices.entity.DeviceStatus;
import com.diasmart.springapi.patients.repository.PatientRepository;
import com.diasmart.springapi.relationships.service.PatientAccessService;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.entity.AppUser;
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
import java.time.OffsetDateTime;
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

        @Mock
        private CurrentUserService currentUserService;

        @Mock
        private DeviceActivationAttemptService activationAttemptService;

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

        private PatientDeviceActivationRequestDTO activationRequest() {
                PatientDeviceActivationRequestDTO request = new PatientDeviceActivationRequestDTO();
                request.setOuterGatewayId("OUT-1");
                request.setInnerUnitId("INN-1");
                request.setPenUnitId("PEN-1");
                request.setGlucoseMeterId("GLU-1");
                return request;
        }

        private void stubActiveUser() {
                AppUser user = currentUser(UserRole.PATIENT);

                when(currentUserService.getCurrentUser())
                                .thenReturn(user);
                when(activationAttemptService.isRateLimited(eq(7L), eq("203.0.113.10"), any(OffsetDateTime.class)))
                                .thenReturn(false);
                lenient().when(patientRepository.existsById(PATIENT_ID))
                                .thenReturn(true);
        }

        private AppUser currentUser(UserRole role) {
                AppUser user = new AppUser();
                user.setUserId(7L);
                user.setRole(role);
                user.setActive(true);
                return user;
        }

        private void assertActivationBadRequest(
                        PatientDeviceActivationRequestDTO request,
                        String expectedErrorCode) {
                stubActiveUser();

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                request,
                                                "203.0.113.10"));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
                assertEquals(expectedErrorCode, exception.getErrorCode());
                verify(deviceRepository, never()).findByDeviceUid(anyString());
                verifyFailureCategory(DeviceActivationFailureCategory.INVALID_KIT);
        }

        private ActivationKitFixture stubValidActivationKit() {
                ActivationKitFixture fixture = validActivationFixture();
                stubValidActivationKit(fixture);
                return fixture;
        }

        private void stubValidActivationKit(ActivationKitFixture fixture) {
                List<DeviceKitDevice> memberships = List.of(
                                membership(77L, 1L, "OUTER_GATEWAY"),
                                membership(77L, 2L, "INNER_UNIT"),
                                membership(77L, 3L, "DOSE_CAP"),
                                membership(77L, 4L, "GLUCOMETER"));

                stubDeviceLookups(fixture);
                lenient().when(deviceKitDeviceRepository.findByDeviceIdIn(any()))
                                .thenReturn(memberships);
                lenient().when(deviceKitRepository.findById(77L))
                                .thenReturn(Optional.of(fixture.kit()));
                lenient().when(buyerRepository.existsById(50L))
                                .thenReturn(true);
                lenient().when(deviceKitDeviceRepository.findByDeviceKitId(77L))
                                .thenReturn(memberships);
                lenient().when(deviceKitRepository.save(any(DeviceKit.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                lenient().when(deviceRepository.save(any(Device.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
        }

        private void stubDeviceLookups(ActivationKitFixture fixture) {
                lenient().when(deviceRepository.findByDeviceUid("OUT-1"))
                                .thenReturn(Optional.of(fixture.outer()));
                lenient().when(deviceRepository.findByDeviceUid("INN-1"))
                                .thenReturn(Optional.of(fixture.inner()));
                lenient().when(deviceRepository.findByDeviceUid("PEN-1"))
                                .thenReturn(Optional.of(fixture.pen()));
                lenient().when(deviceRepository.findByDeviceUid("GLU-1"))
                                .thenReturn(Optional.of(fixture.glucometer()));
        }

        private ActivationKitFixture validActivationFixture() {
                Device outer = activationDevice(1L, "OUT-1", "OUTER_GATEWAY");
                Device inner = activationDevice(2L, "INN-1", "INNER_UNIT");
                Device pen = activationDevice(3L, "PEN-1", "DOSE_CAP");
                Device glucometer = activationDevice(4L, "GLU-1", "GLUCOMETER");
                DeviceKit kit = createKit();

                return new ActivationKitFixture(
                                kit,
                                outer,
                                inner,
                                pen,
                                glucometer);
        }

        private Device activationDevice(Long id, String uid, String deviceType) {
                Device device = new Device();
                device.setDeviceId(id);
                device.setDeviceUid(uid);
                device.setDeviceType(deviceType);
                device.setDeviceName(deviceType);
                device.setBuyerId(50L);
                device.setActive(true);
                device.setStatus(DeviceStatus.AVAILABLE);
                return device;
        }

        private DeviceKitDevice membership(Long kitId, Long deviceId, String role) {
                DeviceKitDevice membership = new DeviceKitDevice();
                membership.setDeviceKitId(kitId);
                membership.setDeviceId(deviceId);
                membership.setKitDeviceRole(role);
                return membership;
        }

        private void verifyFailureCategory(DeviceActivationFailureCategory expectedCategory) {
                verify(activationAttemptService).recordFailure(
                                eq(7L),
                                anyLong(),
                                any(),
                                eq("203.0.113.10"),
                                eq(expectedCategory),
                                argThat(fingerprint -> fingerprint != null && fingerprint.length() == 64),
                                any(OffsetDateTime.class),
                                any());
        }

        private record ActivationKitFixture(
                        DeviceKit kit,
                        Device outer,
                        Device inner,
                        Device pen,
                        Device glucometer) {

                List<Device> devices() {
                        return List.of(outer, inner, pen, glucometer);
                }
        }

        @Test
        void activateDeviceKitShouldAssignCompleteValidKit() {
                stubActiveUser();
                ActivationKitFixture fixture = stubValidActivationKit();

                DeviceKitActivationResponseDTO response = deviceService.activateDeviceKit(
                                PATIENT_ID,
                                activationRequest(),
                                "203.0.113.10");

                assertEquals("ACTIVATED", response.getActivationStatus());
                assertEquals(PATIENT_ID, response.getPatientId());
                assertEquals(77L, response.getKitId());
                assertEquals("KIT-1", response.getKitUid());
                assertNotNull(response.getActivatedAt());
                assertEquals("OUT-1", response.getDevices().getOuterDeviceUid());
                assertEquals("INN-1", response.getDevices().getInnerDeviceUid());
                assertEquals("PEN-1", response.getDevices().getPenDeviceUid());
                assertEquals("GLU-1", response.getDevices().getGlucometerDeviceUid());

                assertEquals(PATIENT_ID, fixture.kit().getPatientId());
                assertNotNull(fixture.kit().getActivatedAt());
                assertTrue(fixture.devices().stream()
                                .allMatch(device -> PATIENT_ID.equals(device.getPatientId())
                                                && device.getStatus() == DeviceStatus.CONNECTED));

                verify(deviceRepository, times(4)).save(any(Device.class));
                verify(deviceKitRepository).save(fixture.kit());
                verify(auditService).logDeviceKitActivated(
                                7L,
                                PATIENT_ID,
                                77L,
                                "KIT-1",
                                "203.0.113.10");
                verify(activationAttemptService).recordSuccess(
                                eq(7L),
                                eq(PATIENT_ID),
                                eq(77L),
                                eq("203.0.113.10"),
                                argThat(fingerprint -> fingerprint != null && fingerprint.length() == 64),
                                any(OffsetDateTime.class));
        }

        @Test
        void activateDeviceKitShouldRejectMissingOuterUid() {
                PatientDeviceActivationRequestDTO request = activationRequest();
                request.setOuterGatewayId(null);

                assertActivationBadRequest(request, "KIT_REQUIRES_ALL_DEVICES");
        }

        @Test
        void activateDeviceKitShouldRejectMissingInnerUid() {
                PatientDeviceActivationRequestDTO request = activationRequest();
                request.setInnerUnitId(null);

                assertActivationBadRequest(request, "KIT_REQUIRES_ALL_DEVICES");
        }

        @Test
        void activateDeviceKitShouldRejectMissingPenUid() {
                PatientDeviceActivationRequestDTO request = activationRequest();
                request.setPenUnitId(null);

                assertActivationBadRequest(request, "KIT_REQUIRES_ALL_DEVICES");
        }

        @Test
        void activateDeviceKitShouldRejectMissingGlucometerUid() {
                PatientDeviceActivationRequestDTO request = activationRequest();
                request.setGlucoseMeterId(null);

                assertActivationBadRequest(request, "KIT_REQUIRES_ALL_DEVICES");
        }

        @Test
        void activateDeviceKitShouldRejectBlankUid() {
                PatientDeviceActivationRequestDTO request = activationRequest();
                request.setInnerUnitId("  ");

                assertActivationBadRequest(request, "KIT_REQUIRES_ALL_DEVICES");
        }

        @Test
        void activateDeviceKitShouldRejectDuplicateUidValues() {
                PatientDeviceActivationRequestDTO request = activationRequest();
                request.setGlucoseMeterId("OUT-1");

                assertActivationBadRequest(request, "DUPLICATE_DEVICE_UID_IN_KIT");
        }

        @Test
        void activateDeviceKitShouldRejectUnknownUidWithGenericMessage() {
                stubActiveUser();

                when(deviceRepository.findByDeviceUid("OUT-1"))
                                .thenReturn(Optional.empty());

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                activationRequest(),
                                                "203.0.113.10"));

                assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
                assertEquals("INVALID_DEVICE_KIT", exception.getErrorCode());
                assertEquals("The entered device kit information is invalid.", exception.getMessage());
                verify(deviceRepository, never()).save(any(Device.class));
                verify(auditService, never()).logDeviceKitActivated(any(), any(), any(), any(), any());
                verifyFailureCategory(DeviceActivationFailureCategory.INVALID_KIT);
        }

        @Test
        void activateDeviceKitShouldRejectInactiveDeviceWithGenericMessage() {
                stubActiveUser();
                Device inactiveOuter = activationDevice(1L, "OUT-1", "OUTER_GATEWAY");
                inactiveOuter.setActive(false);

                when(deviceRepository.findByDeviceUid("OUT-1"))
                                .thenReturn(Optional.of(inactiveOuter));

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                activationRequest(),
                                                "203.0.113.10"));

                assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
                assertEquals("INVALID_DEVICE_KIT", exception.getErrorCode());
                assertEquals("The entered device kit information is invalid.", exception.getMessage());
                verifyFailureCategory(DeviceActivationFailureCategory.INACTIVE_DEVICE);
        }

        @Test
        void activateDeviceKitShouldRejectOuterFieldContainingInnerDevice() {
                stubActiveUser();
                Device wrongOuter = activationDevice(1L, "OUT-1", "INNER_UNIT");

                when(deviceRepository.findByDeviceUid("OUT-1"))
                                .thenReturn(Optional.of(wrongOuter));

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                activationRequest(),
                                                "203.0.113.10"));

                assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
                assertEquals("INVALID_DEVICE_KIT", exception.getErrorCode());
                verifyFailureCategory(DeviceActivationFailureCategory.TYPE_MISMATCH);
        }

        @Test
        void activateDeviceKitShouldRejectInnerFieldContainingOuterDevice() {
                stubActiveUser();
                ActivationKitFixture fixture = validActivationFixture();
                fixture.inner().setDeviceType("OUTER_GATEWAY");
                stubDeviceLookups(fixture);

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                activationRequest(),
                                                "203.0.113.10"));

                assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
                assertEquals("INVALID_DEVICE_KIT", exception.getErrorCode());
                verifyFailureCategory(DeviceActivationFailureCategory.TYPE_MISMATCH);
        }

        @Test
        void activateDeviceKitShouldRejectPenFieldContainingWrongType() {
                stubActiveUser();
                ActivationKitFixture fixture = validActivationFixture();
                fixture.pen().setDeviceType("GLUCOMETER");
                stubDeviceLookups(fixture);

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                activationRequest(),
                                                "203.0.113.10"));

                assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
                verifyFailureCategory(DeviceActivationFailureCategory.TYPE_MISMATCH);
        }

        @Test
        void activateDeviceKitShouldRejectGlucometerFieldContainingWrongType() {
                stubActiveUser();
                ActivationKitFixture fixture = validActivationFixture();
                fixture.glucometer().setDeviceType("DOSE_CAP");
                stubDeviceLookups(fixture);

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                activationRequest(),
                                                "203.0.113.10"));

                assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
                verifyFailureCategory(DeviceActivationFailureCategory.TYPE_MISMATCH);
        }

        @Test
        void activateDeviceKitShouldRejectDevicesFromDifferentKits() {
                stubActiveUser();
                ActivationKitFixture fixture = validActivationFixture();
                stubDeviceLookups(fixture);
                when(deviceKitDeviceRepository.findByDeviceIdIn(any()))
                                .thenReturn(List.of(
                                                membership(77L, 1L, "OUTER_GATEWAY"),
                                                membership(78L, 2L, "INNER_UNIT"),
                                                membership(77L, 3L, "DOSE_CAP"),
                                                membership(77L, 4L, "GLUCOMETER")));

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                activationRequest(),
                                                "203.0.113.10"));

                assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
                assertEquals("The entered device kit information is invalid.", exception.getMessage());
                verify(deviceKitRepository, never()).save(any(DeviceKit.class));
        }

        @Test
        void activateDeviceKitShouldRejectLegacyUngroupedDevice() {
                stubActiveUser();
                ActivationKitFixture fixture = validActivationFixture();
                stubDeviceLookups(fixture);
                when(deviceKitDeviceRepository.findByDeviceIdIn(any()))
                                .thenReturn(List.of());

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                activationRequest(),
                                                "203.0.113.10"));

                assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
                assertEquals("INVALID_DEVICE_KIT", exception.getErrorCode());
        }

        @Test
        void activateDeviceKitShouldRejectIncompleteKit() {
                stubActiveUser();
                ActivationKitFixture fixture = stubValidActivationKit();
                when(deviceKitDeviceRepository.findByDeviceKitId(77L))
                                .thenReturn(List.of(
                                                membership(77L, 1L, "OUTER_GATEWAY"),
                                                membership(77L, 2L, "INNER_UNIT"),
                                                membership(77L, 3L, "DOSE_CAP")));

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                activationRequest(),
                                                "203.0.113.10"));

                assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
                verify(deviceRepository, never()).save(any(Device.class));
        }

        @Test
        void activateDeviceKitShouldRejectDuplicateRoleInKit() {
                stubActiveUser();
                ActivationKitFixture fixture = stubValidActivationKit();
                when(deviceKitDeviceRepository.findByDeviceKitId(77L))
                                .thenReturn(List.of(
                                                membership(77L, 1L, "OUTER_GATEWAY"),
                                                membership(77L, 2L, "INNER_UNIT"),
                                                membership(77L, 3L, "DOSE_CAP"),
                                                membership(77L, 4L, "DOSE_CAP")));

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                activationRequest(),
                                                "203.0.113.10"));

                assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
                verify(deviceRepository, never()).save(any(Device.class));
        }

        @Test
        void activateDeviceKitShouldRejectInactiveKit() {
                stubActiveUser();
                ActivationKitFixture fixture = validActivationFixture();
                fixture.kit().setStatus("INACTIVE");
                stubValidActivationKit(fixture);

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                activationRequest(),
                                                "203.0.113.10"));

                assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
                verify(deviceRepository, never()).save(any(Device.class));
        }

        @Test
        void activateDeviceKitShouldRejectDevicesFromDifferentBuyers() {
                stubActiveUser();
                ActivationKitFixture fixture = validActivationFixture();
                fixture.glucometer().setBuyerId(51L);
                stubValidActivationKit(fixture);

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                activationRequest(),
                                                "203.0.113.10"));

                assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
                assertEquals("The entered device kit information is invalid.", exception.getMessage());
        }

        @Test
        void activateDeviceKitShouldRejectDeviceAssignedToAnotherPatient() {
                stubActiveUser();
                ActivationKitFixture fixture = validActivationFixture();
                fixture.inner().setPatientId(999L);
                stubValidActivationKit(fixture);

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                activationRequest(),
                                                "203.0.113.10"));

                assertEquals(HttpStatus.CONFLICT, exception.getStatus());
                assertEquals("DEVICE_ALREADY_ASSIGNED", exception.getErrorCode());
                verifyFailureCategory(DeviceActivationFailureCategory.DEVICE_CONFLICT);
        }

        @Test
        void activateDeviceKitShouldRejectPartiallyAssignedSamePatientKit() {
                stubActiveUser();
                ActivationKitFixture fixture = validActivationFixture();
                fixture.outer().setPatientId(PATIENT_ID);
                stubValidActivationKit(fixture);

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                activationRequest(),
                                                "203.0.113.10"));

                assertEquals(HttpStatus.CONFLICT, exception.getStatus());
                assertEquals("DEVICE_KIT_PARTIALLY_ASSIGNED", exception.getErrorCode());
                verify(deviceRepository, never()).save(any(Device.class));
        }

        @Test
        void activateDeviceKitShouldBeIdempotentWhenAllDevicesAlreadyAssignedToSamePatient() {
                stubActiveUser();
                ActivationKitFixture fixture = validActivationFixture();
                fixture.kit().setPatientId(PATIENT_ID);
                fixture.kit().setActivatedAt(OffsetDateTime.parse("2026-08-02T12:00:00Z"));
                fixture.devices().forEach(device -> device.setPatientId(PATIENT_ID));
                stubValidActivationKit(fixture);

                DeviceKitActivationResponseDTO response = deviceService.activateDeviceKit(
                                PATIENT_ID,
                                activationRequest(),
                                "203.0.113.10");

                assertEquals("ALREADY_ACTIVE", response.getActivationStatus());
                assertEquals(OffsetDateTime.parse("2026-08-02T12:00:00Z"), response.getActivatedAt());
                verify(deviceRepository, never()).save(any(Device.class));
                verify(auditService, never()).logDeviceKitActivated(any(), any(), any(), any(), any());
                verify(auditService).logDeviceKitAlreadyActive(
                                7L,
                                PATIENT_ID,
                                77L,
                                "KIT-1",
                                "203.0.113.10");
        }

        @Test
        void activateDeviceKitShouldRejectInactiveUser() {
                AppUser inactiveUser = currentUser(UserRole.PATIENT);
                inactiveUser.setActive(false);

                when(currentUserService.getCurrentUser())
                                .thenReturn(inactiveUser);

                assertThrows(
                                AccessDeniedException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                activationRequest(),
                                                "203.0.113.10"));

                verify(patientAccessService, never()).requireCanManagePatientDevices(anyLong());
                verify(deviceRepository, never()).findByDeviceUid(anyString());
                verifyFailureCategory(DeviceActivationFailureCategory.UNAUTHORIZED_PATIENT);
        }

        @Test
        void activateDeviceKitShouldRejectUnauthorizedPatient() {
                stubActiveUser();
                doThrow(new AccessDeniedException("denied"))
                                .when(patientAccessService)
                                .requireCanManagePatientDevices(999L);

                assertThrows(
                                AccessDeniedException.class,
                                () -> deviceService.activateDeviceKit(
                                                999L,
                                                activationRequest(),
                                                "203.0.113.10"));

                verify(deviceRepository, never()).findByDeviceUid(anyString());
                verifyFailureCategory(DeviceActivationFailureCategory.UNAUTHORIZED_PATIENT);
        }

        @Test
        void activateDeviceKitShouldEnforceRateLimitBeforeDeviceLookup() {
                AppUser user = currentUser(UserRole.PATIENT);

                when(currentUserService.getCurrentUser())
                                .thenReturn(user);
                when(activationAttemptService.isRateLimited(eq(7L), eq("203.0.113.10"), any(OffsetDateTime.class)))
                                .thenReturn(true);
                when(activationAttemptService.blockedUntil(any(OffsetDateTime.class)))
                                .thenReturn(OffsetDateTime.parse("2026-08-02T12:15:00Z"));

                ApiException exception = assertThrows(
                                ApiException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                activationRequest(),
                                                "203.0.113.10"));

                assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatus());
                assertEquals("ACTIVATION_RATE_LIMIT_EXCEEDED", exception.getErrorCode());
                verify(patientAccessService, never()).requireCanManagePatientDevices(anyLong());
                verify(deviceRepository, never()).findByDeviceUid(anyString());
                verifyFailureCategory(DeviceActivationFailureCategory.RATE_LIMITED);
        }

        @Test
        void activateDeviceKitShouldNotCreateSuccessAuditWhenDeviceLookupFails() {
                stubActiveUser();

                when(deviceRepository.findByDeviceUid("OUT-1"))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ApiException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                activationRequest(),
                                                "203.0.113.10"));

                verify(auditService, never()).logDeviceKitActivated(any(), any(), any(), any(), any());
                verify(activationAttemptService, never()).recordSuccess(any(), any(), any(), any(), any(), any());
        }

        @Test
        void activateDeviceKitShouldNotStoreRawUidsInAttemptRecord() {
                stubActiveUser();

                when(deviceRepository.findByDeviceUid("OUT-1"))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ApiException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                activationRequest(),
                                                "203.0.113.10"));

                ArgumentCaptor<String> fingerprintCaptor = ArgumentCaptor.forClass(String.class);
                verify(activationAttemptService).recordFailure(
                                eq(7L),
                                eq(PATIENT_ID),
                                isNull(),
                                eq("203.0.113.10"),
                                eq(DeviceActivationFailureCategory.INVALID_KIT),
                                fingerprintCaptor.capture(),
                                any(OffsetDateTime.class),
                                isNull());
                assertEquals(64, fingerprintCaptor.getValue().length());
                assertFalse(fingerprintCaptor.getValue().contains("OUT-1"));
        }

        @Test
        void activateDeviceKitShouldNotExposeBuyerOrCredentialFieldsInResponse() throws Exception {
                stubActiveUser();
                stubValidActivationKit();

                DeviceKitActivationResponseDTO response = deviceService.activateDeviceKit(
                                PATIENT_ID,
                                activationRequest(),
                                "203.0.113.10");

                String responseSurface = java.util.Arrays.toString(
                                DeviceKitActivationResponseDTO.class.getMethods()).toLowerCase()
                                + java.util.Arrays.toString(
                                                DeviceKitActivationResponseDTO.ActivationDevicesDTO.class.getMethods()).toLowerCase();

                assertNotNull(response);
                assertFalse(responseSurface.contains("buyer"));
                assertFalse(responseSurface.contains("nic"));
                assertFalse(responseSurface.contains("contact"));
                assertFalse(responseSurface.contains("address"));
                assertFalse(responseSurface.contains("password"));
                assertFalse(responseSurface.contains("certificate"));
                assertFalse(responseSurface.contains("jwt"));
        }

        @Test
        void activateDeviceKitShouldStopWhenThirdDeviceSaveFails() {
                stubActiveUser();
                stubValidActivationKit();
                when(deviceRepository.save(any(Device.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0))
                                .thenAnswer(invocation -> invocation.getArgument(0))
                                .thenThrow(new RuntimeException("third save failed"));

                RuntimeException exception = assertThrows(
                                RuntimeException.class,
                                () -> deviceService.activateDeviceKit(
                                                PATIENT_ID,
                                                activationRequest(),
                                                "203.0.113.10"));

                assertEquals("third save failed", exception.getMessage());
                verify(auditService, never()).logDeviceKitActivated(any(), any(), any(), any(), any());
                verify(activationAttemptService, never()).recordSuccess(any(), any(), any(), any(), any(), any());
        }

        @Test
        void activateDeviceKitShouldBeTransactionalForRollback() throws Exception {
                Transactional transactional = DeviceServiceImpl.class
                                .getMethod(
                                                "activateDeviceKit",
                                                Long.class,
                                                PatientDeviceActivationRequestDTO.class,
                                                String.class)
                                .getAnnotation(Transactional.class);

                assertNotNull(transactional);
        }
}
