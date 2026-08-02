package com.diasmart.springapi.devices.service.impl;

import com.diasmart.springapi.audit.service.AuditService;
import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.devices.dto.AssignDeviceRequestDTO;
import com.diasmart.springapi.devices.dto.DeviceDiagnosticsDTO;
import com.diasmart.springapi.devices.dto.DeviceKitDTO;
import com.diasmart.springapi.devices.dto.DeviceKitRegistrationRequestDTO;
import com.diasmart.springapi.devices.dto.DeviceReplayStatisticsDTO;
import com.diasmart.springapi.devices.dto.DeviceResponseDTO;
import com.diasmart.springapi.devices.dto.DeviceSummaryDTO;
import com.diasmart.springapi.devices.dto.PatientDeviceSummaryDTO;
import com.diasmart.springapi.devices.dto.RegisterDeviceRequestDTO;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.entity.DeviceHealthLog;
import com.diasmart.springapi.devices.entity.DeviceKit;
import com.diasmart.springapi.devices.entity.DeviceKitDevice;
import com.diasmart.springapi.devices.repository.DeviceHealthLogRepository;
import com.diasmart.springapi.devices.repository.DeviceKitDeviceRepository;
import com.diasmart.springapi.devices.repository.DeviceKitRepository;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.devices.service.DeviceService;
import com.diasmart.springapi.relationships.service.PatientAccessService;
import com.diasmart.springapi.raw_events.entity.RawDeviceEvent;
import com.diasmart.springapi.raw_events.repository.RawDeviceEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.LocalDate;
import com.diasmart.springapi.devices.dto.BuyerDeviceKitsDTO;
import com.diasmart.springapi.devices.entity.Buyer;
import com.diasmart.springapi.devices.entity.DeviceStatus;
import com.diasmart.springapi.devices.repository.BuyerRepository;
import com.diasmart.springapi.patients.repository.PatientRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeviceServiceImpl implements DeviceService {

        private static final int OFFLINE_AFTER_MINUTES = 15;

        private static final Set<String> DEVICE_TYPES = Set.of(
                        "INNER_UNIT",
                        "OUTER_GATEWAY",
                        "PEN_UNIT",
                        "DOSE_CAP",
                        "GLUCOMETER",
                        "OTHER");

        private static final Set<String> COMMUNICATION_TYPES = Set.of(
                        "BLE",
                        "ESP_NOW",
                        "MQTTS",
                        "HTTPS",
                        "MANUAL",
                        "OTHER");

        private static final String KIT_STATUS_ACTIVE = "ACTIVE";

        private static final List<KitDeviceDefinition> KIT_DEVICE_DEFINITIONS = List.of(
                        new KitDeviceDefinition(
                                        "OUTER_GATEWAY",
                                        "OUTER_GATEWAY",
                                        "DiaSmart Outer Gateway",
                                        "MQTTS"),
                        new KitDeviceDefinition(
                                        "INNER_UNIT",
                                        "INNER_UNIT",
                                        "DiaSmart Inner Unit",
                                        "ESP_NOW"),
                        new KitDeviceDefinition(
                                        "DOSE_CAP",
                                        "DOSE_CAP",
                                        "DiaSmart Pen Unit",
                                        "BLE"),
                        new KitDeviceDefinition(
                                        "GLUCOMETER",
                                        "GLUCOMETER",
                                        "DiaSmart Glucose Meter",
                                        "BLE"));

        private static final Set<String> KIT_DEVICE_ROLES = KIT_DEVICE_DEFINITIONS
                        .stream()
                        .map(KitDeviceDefinition::role)
                        .collect(Collectors.toUnmodifiableSet());

        private final DeviceRepository deviceRepository;
        private final DeviceHealthLogRepository healthLogRepository;
        private final RawDeviceEventRepository rawDeviceEventRepository;
        private final AuditService auditService;
        private final BuyerRepository buyerRepository;
        private final PatientRepository patientRepository;
        private final DeviceKitRepository deviceKitRepository;
        private final DeviceKitDeviceRepository deviceKitDeviceRepository;
        private final PatientAccessService patientAccessService;

        public DeviceServiceImpl(
                        DeviceRepository deviceRepository,
                        DeviceHealthLogRepository healthLogRepository,
                        RawDeviceEventRepository rawDeviceEventRepository,
                        AuditService auditService,
                        BuyerRepository buyerRepository,
                        PatientRepository patientRepository,
                        DeviceKitRepository deviceKitRepository,
                        DeviceKitDeviceRepository deviceKitDeviceRepository,
                        PatientAccessService patientAccessService) {
                this.deviceRepository = deviceRepository;
                this.healthLogRepository = healthLogRepository;
                this.rawDeviceEventRepository = rawDeviceEventRepository;
                this.auditService = auditService;
                this.buyerRepository = buyerRepository;
                this.patientRepository = patientRepository;
                this.deviceKitRepository = deviceKitRepository;
                this.deviceKitDeviceRepository = deviceKitDeviceRepository;
                this.patientAccessService = patientAccessService;
        }

        @Override
        public java.util.List<DeviceSummaryDTO> getAllDevices() {
                return deviceRepository
                                .findAllByOrderByDeviceIdAsc()
                                .stream()
                                .map(this::mapToSummaryDTO)
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public List<PatientDeviceSummaryDTO> getPatientDevices(Long patientId) {
                patientAccessService.requireCanViewPatient(patientId);
                return deviceRepository
                                .findByPatientIdOrderByDeviceIdAsc(patientId)
                                .stream()
                                .map(this::mapToPatientDeviceSummaryDTO)
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public List<BuyerDeviceKitsDTO> getDeviceKits() {
                List<DeviceKit> kits = deviceKitRepository.findAllByOrderByCreatedAtDesc();

                if (kits.isEmpty()) {
                        return List.of();
                }

                Set<Long> buyerIds = kits.stream()
                                .map(DeviceKit::getBuyerId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());
                Map<Long, Buyer> buyersById = buyerRepository.findAllById(buyerIds)
                                .stream()
                                .collect(Collectors.toMap(Buyer::getBuyerId, Function.identity()));

                List<Long> kitIds = kits.stream()
                                .map(DeviceKit::getDeviceKitId)
                                .filter(Objects::nonNull)
                                .toList();
                List<DeviceKitDevice> memberships = kitIds.isEmpty()
                                ? List.of()
                                : deviceKitDeviceRepository.findByDeviceKitIdIn(kitIds);
                Map<Long, List<DeviceKitDevice>> membershipsByKit = memberships.stream()
                                .collect(Collectors.groupingBy(DeviceKitDevice::getDeviceKitId));

                Set<Long> deviceIds = memberships.stream()
                                .map(DeviceKitDevice::getDeviceId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());
                Map<Long, Device> devicesById = deviceIds.isEmpty()
                                ? Map.of()
                                : deviceRepository.findAllById(deviceIds)
                                                .stream()
                                                .collect(Collectors.toMap(
                                                                Device::getDeviceId,
                                                                Function.identity()));

                Map<Long, List<DeviceKit>> kitsByBuyer = kits.stream()
                                .filter(kit -> kit.getBuyerId() != null)
                                .collect(Collectors.groupingBy(
                                                DeviceKit::getBuyerId,
                                                LinkedHashMap::new,
                                                Collectors.toList()));

                return kitsByBuyer.entrySet().stream()
                                .map(entry -> mapToBuyerDeviceKitsDTO(
                                                buyersById.get(entry.getKey()),
                                                entry.getValue(),
                                                membershipsByKit,
                                                devicesById))
                                .filter(Objects::nonNull)
                                .toList();
        }

        @Override
        public DeviceResponseDTO getDeviceById(Long id) {
                Device device = findDevice(id);
                return mapToDTO(device);
        }

        @Override
        public DeviceResponseDTO registerDevice(
                        RegisterDeviceRequestDTO dto) {
                String deviceUid = requireText(
                                firstNonBlank(
                                                dto.getDeviceUid(),
                                                dto.getDeviceId()),
                                "deviceUid");

                if (deviceRepository.findByDeviceUid(deviceUid).isPresent()) {
                        throw new ApiException(
                                        HttpStatus.CONFLICT,
                                        "DEVICE_ALREADY_EXISTS",
                                        "Device already exists");
                }

                assertUniqueOptional(
                                "awsThingName",
                                dto.getAwsThingName(),
                                deviceRepository::findByAwsThingName);
                assertUniqueOptional(
                                "mqttClientId",
                                dto.getMqttClientId(),
                                deviceRepository::findByMqttClientId);
                assertUniqueOptional(
                                "macAddress",
                                dto.getMacAddress(),
                                deviceRepository::findByMacAddress);
                assertUniqueOptional(
                                "serialNumber",
                                dto.getSerialNumber(),
                                deviceRepository::findBySerialNumber);

                Device device = new Device();

                device.setDeviceUid(deviceUid);
                device.setAwsThingName(trimToNull(dto.getAwsThingName()));
                device.setMqttClientId(trimToNull(dto.getMqttClientId()));
                device.setMacAddress(trimToNull(dto.getMacAddress()));
                device.setSerialNumber(trimToNull(dto.getSerialNumber()));
                device.setDeviceType(
                                normalizeRequired(
                                                dto.getDeviceType(),
                                                DEVICE_TYPES,
                                                "deviceType"));
                device.setDeviceName(trimToNull(dto.getDeviceName()));
                device.setCommunicationType(
                                normalizeOptional(
                                                dto.getCommunicationType(),
                                                COMMUNICATION_TYPES,
                                                "OTHER"));
                device.setFirmwareVersion(
                                trimToNull(dto.getFirmwareVersion()));
                device.setHardwareVersion(
                                trimToNull(dto.getHardwareVersion()));
                device.setNotes(trimToNull(dto.getNotes()));
                device.setActive(true);

                Device savedDevice = deviceRepository.save(device);
                auditService.logDeviceRegistration(savedDevice);

                return mapToDTO(savedDevice);
        }

        @Override
        @Transactional
        public DeviceKitDTO registerDeviceKit(DeviceKitRegistrationRequestDTO dto) {
                if (dto == null) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "VALIDATION_ERROR",
                                        "Device kit registration request is required");
                }

                Map<String, String> deviceUidsByRole = requireCompleteKitDeviceUids(dto);
                deviceUidsByRole.forEach(this::checkDeviceDoesNotExist);

                Buyer buyer = getOrCreateBuyer(dto);

                DeviceKit deviceKit = new DeviceKit();
                deviceKit.setKitUid(resolveKitUid(dto.getKitUid()));
                deviceKit.setBuyerId(buyer.getBuyerId());
                deviceKit.setPurchaseDate(resolvePurchaseDate(dto.getPurchaseDate()));
                deviceKit.setStatus(KIT_STATUS_ACTIVE);

                DeviceKit savedKit = deviceKitRepository.save(deviceKit);

                Map<Long, Device> devicesById = new LinkedHashMap<>();
                List<DeviceKitDevice> memberships = new ArrayList<>();

                for (KitDeviceDefinition definition : KIT_DEVICE_DEFINITIONS) {
                        Device savedDevice = createDevice(
                                        deviceUidsByRole.get(definition.role()),
                                        definition.deviceType(),
                                        definition.deviceName(),
                                        definition.communicationType(),
                                        buyer.getBuyerId());
                        auditService.logDeviceRegistration(savedDevice);

                        DeviceKitDevice membership = addDeviceToKit(
                                        savedKit,
                                        savedDevice,
                                        definition.role());
                        memberships.add(membership);
                        devicesById.put(savedDevice.getDeviceId(), savedDevice);
                }

                return mapToKitDTO(savedKit, memberships, devicesById);
        }

        private Map<String, String> requireCompleteKitDeviceUids(DeviceKitRegistrationRequestDTO dto) {
                Map<String, String> deviceUidsByRole = new LinkedHashMap<>();
                deviceUidsByRole.put("OUTER_GATEWAY", trimToNull(dto.getOuterGatewayId()));
                deviceUidsByRole.put("INNER_UNIT", trimToNull(dto.getInnerUnitId()));
                deviceUidsByRole.put("DOSE_CAP", trimToNull(dto.getPenUnitId()));
                deviceUidsByRole.put("GLUCOMETER", trimToNull(dto.getGlucoseMeterId()));

                if (deviceUidsByRole.values().stream().anyMatch(Objects::isNull)) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "KIT_REQUIRES_ALL_DEVICES",
                                        "A complete kit requires outer gateway, inner unit, dose cap, and glucometer device IDs.");
                }

                if (new HashSet<>(deviceUidsByRole.values()).size() != deviceUidsByRole.size()) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "DUPLICATE_DEVICE_UID_IN_KIT",
                                        "Each kit device ID must be unique.");
                }

                return deviceUidsByRole;
        }

        private void checkDeviceDoesNotExist(String role, String uid) {
                if (deviceRepository.existsByDeviceUid(uid)) {
                        throw new ApiException(
                                        HttpStatus.CONFLICT,
                                        "DEVICE_ALREADY_EXISTS",
                                        role + " with ID " + uid + " already exists.");
                }
        }

        private Buyer getOrCreateBuyer(DeviceKitRegistrationRequestDTO dto) {
                String nic = requireText(dto.getNic(), "nic");
                return buyerRepository.findByNic(nic).orElseGet(() -> {
                        Buyer newBuyer = new Buyer();
                        newBuyer.setFullName(requireText(dto.getBuyerFullName(), "buyerFullName"));
                        newBuyer.setNic(nic);
                        newBuyer.setContactNumber(requireText(dto.getContactNumber(), "contactNumber"));
                        newBuyer.setAddress(trimToNull(dto.getAddress()));
                        newBuyer.setPurchaseDate(resolvePurchaseDate(dto.getPurchaseDate()));
                        return buyerRepository.save(newBuyer);
                });
        }

        private String resolveKitUid(String requestedKitUid) {
                String normalized = trimToNull(requestedKitUid);

                if (normalized != null) {
                        if (deviceKitRepository.existsByKitUid(normalized)) {
                                throw new ApiException(
                                                HttpStatus.CONFLICT,
                                                "DEVICE_KIT_ALREADY_EXISTS",
                                                "Device kit UID already exists.");
                        }

                        return normalized;
                }

                for (int attempt = 0; attempt < 5; attempt++) {
                        String generated = "KIT-" + UUID.randomUUID();

                        if (!deviceKitRepository.existsByKitUid(generated)) {
                                return generated;
                        }
                }

                throw new ApiException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "DEVICE_KIT_UID_GENERATION_FAILED",
                                "Unable to generate a unique device kit UID.");
        }

        private LocalDate resolvePurchaseDate(LocalDate purchaseDate) {
                return purchaseDate == null ? LocalDate.now(ZoneOffset.UTC) : purchaseDate;
        }

        private Device createDevice(String uid, String type, String name, String commType, Long buyerId) {
                Device d = new Device();
                d.setDeviceUid(uid);
                d.setDeviceType(type);
                d.setDeviceName(name);
                d.setCommunicationType(commType);
                d.setBuyerId(buyerId);
                d.setPatientId(null);
                d.setActive(true);
                d.setStatus(DeviceStatus.UNKNOWN);
                return deviceRepository.save(d);
        }

        DeviceKitDevice addDeviceToKit(DeviceKit kit, Device device, String kitDeviceRole) {
                if (kit == null || kit.getDeviceKitId() == null) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "DEVICE_KIT_REQUIRED",
                                        "A saved device kit is required.");
                }

                if (device == null || device.getDeviceId() == null) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "DEVICE_REQUIRED",
                                        "A saved device is required.");
                }

                String normalizedRole = normalizeRequired(
                                kitDeviceRole,
                                KIT_DEVICE_ROLES,
                                "kitDeviceRole");
                String expectedDeviceType = expectedDeviceTypeForRole(normalizedRole);
                String actualDeviceType = normalize(device.getDeviceType());

                if (!expectedDeviceType.equals(actualDeviceType)) {
                        throw new ApiException(
                                        HttpStatus.UNPROCESSABLE_ENTITY,
                                        "KIT_DEVICE_ROLE_MISMATCH",
                                        "Device type " + device.getDeviceType() + " cannot be registered as " + normalizedRole + ".");
                }

                if (deviceKitDeviceRepository.existsByDeviceKitIdAndKitDeviceRole(
                                kit.getDeviceKitId(),
                                normalizedRole)) {
                        throw new ApiException(
                                        HttpStatus.CONFLICT,
                                        "DEVICE_KIT_ROLE_ALREADY_EXISTS",
                                        "This device kit already has a " + normalizedRole + " device.");
                }

                if (deviceKitDeviceRepository.existsByDeviceId(device.getDeviceId())) {
                        throw new ApiException(
                                        HttpStatus.CONFLICT,
                                        "DEVICE_ALREADY_IN_KIT",
                                        "This device already belongs to a device kit.");
                }

                DeviceKitDevice membership = new DeviceKitDevice();
                membership.setDeviceKitId(kit.getDeviceKitId());
                membership.setDeviceId(device.getDeviceId());
                membership.setKitDeviceRole(normalizedRole);

                return deviceKitDeviceRepository.save(membership);
        }

        private String expectedDeviceTypeForRole(String role) {
                return KIT_DEVICE_DEFINITIONS.stream()
                                .filter(definition -> definition.role().equals(role))
                                .findFirst()
                                .map(KitDeviceDefinition::deviceType)
                                .orElseThrow(() -> new ApiException(
                                                HttpStatus.BAD_REQUEST,
                                                "VALIDATION_ERROR",
                                                "kitDeviceRole is invalid"));
        }

        private Device findDeviceByUid(String uid, String label) {
                return deviceRepository.findByDeviceUid(uid)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DEVICE_NOT_FOUND", label + " ID is invalid or does not exist."));
        }

        @Override
        @Transactional
        public void activateDeviceKit(Long patientId, com.diasmart.springapi.devices.dto.PatientDeviceActivationRequestDTO dto) {
                List<Device> devicesToActivate = new java.util.ArrayList<>();
                if (dto.getOuterGatewayId() != null && !dto.getOuterGatewayId().trim().isEmpty()) {
                        devicesToActivate.add(findDeviceByUid(dto.getOuterGatewayId(), "Outer Gateway"));
                }
                if (dto.getInnerUnitId() != null && !dto.getInnerUnitId().trim().isEmpty()) {
                        devicesToActivate.add(findDeviceByUid(dto.getInnerUnitId(), "Inner Unit"));
                }
                if (dto.getPenUnitId() != null && !dto.getPenUnitId().trim().isEmpty()) {
                        devicesToActivate.add(findDeviceByUid(dto.getPenUnitId(), "Pen Unit"));
                }
                if (dto.getGlucoseMeterId() != null && !dto.getGlucoseMeterId().trim().isEmpty()) {
                        devicesToActivate.add(findDeviceByUid(dto.getGlucoseMeterId(), "Glucose Meter"));
                }

                if (devicesToActivate.isEmpty()) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "NO_DEVICES_PROVIDED", "At least one Device ID must be provided to activate.");
                }

                for (Device device : devicesToActivate) {
                        if (!Boolean.TRUE.equals(device.getActive())) {
                                throw new ApiException(HttpStatus.BAD_REQUEST, "DEVICE_INACTIVE", "Device " + device.getDeviceUid() + " is not active.");
                        }
                        if (device.getPatientId() != null && !device.getPatientId().equals(patientId)) {
                                throw new ApiException(HttpStatus.CONFLICT, "DEVICE_ALREADY_ASSIGNED", "Device " + device.getDeviceUid() + " is already assigned to another patient.");
                        }
                }

                for (Device device : devicesToActivate) {
                        device.setPatientId(patientId);
                        device.setStatus(DeviceStatus.CONNECTED);
                        deviceRepository.save(device);
                }
        }

        @Override
        public DeviceResponseDTO assignDevice(
                        Long id,
                        AssignDeviceRequestDTO dto) {
                Device device = findDevice(id);

                if (!Boolean.TRUE.equals(device.getActive())) {
                        throw new ApiException(
                                        HttpStatus.CONFLICT,
                                        "DEVICE_INACTIVE",
                                        "This device is not active and cannot be assigned.");
                }

                if (device.getBuyerId() == null) {
                        throw new ApiException(
                                        HttpStatus.CONFLICT,
                                        "DEVICE_NOT_REGISTERED",
                                        "This device does not belong to a registered buyer. Please contact your administrator.");
                }

                Long previousPatientId = device.getPatientId();
                Long requestedPatientId = dto.getPatientId();

                if (previousPatientId != null
                                && !previousPatientId.equals(requestedPatientId)) {
                        throw new ApiException(
                                        HttpStatus.CONFLICT,
                                        "DEVICE_ALREADY_ASSIGNED",
                                        "This device is already connected to another patient. Please contact your administrator.");
                }

                if (previousPatientId != null
                                && previousPatientId.equals(requestedPatientId)) {
                        return mapToDTO(device);
                }

                device.setPatientId(requestedPatientId);
                device.setStatus(DeviceStatus.CONNECTED);

                Device updatedDevice = deviceRepository.save(device);
                auditService.logDeviceAssignment(
                                updatedDevice,
                                previousPatientId,
                                requestedPatientId);

                return mapToDTO(updatedDevice);
        }

        @Override
        public DeviceResponseDTO unassignDevice(Long id) {
                Device device = findDevice(id);
                Long previousPatientId = device.getPatientId();

                device.setPatientId(null);

                Device updatedDevice = deviceRepository.save(device);
                auditService.logDeviceUnassignment(updatedDevice, previousPatientId);

                return mapToDTO(updatedDevice);
        }

        @Override
        public DeviceDiagnosticsDTO getDeviceDiagnostics(Long id) {
                Device device = findDevice(id);
                Optional<DeviceHealthLog> latestHealth = healthLogRepository
                                .findTopByDeviceIdOrderByMeasuredAtDesc(
                                                id);
                Optional<RawDeviceEvent> latestRawEvent = rawDeviceEventRepository
                                .findLatestForDeviceDiagnostics(id);

                OffsetDateTime lastMqttReceivedAt = latestRawEvent
                                .map(RawDeviceEvent::getReceivedAt)
                                .orElse(device.getLastSeenAt());
                DeviceHealthLog healthLog = latestHealth.orElse(null);
                String status = resolveStatus(
                                device,
                                healthLog,
                                lastMqttReceivedAt);

                DeviceDiagnosticsDTO dto = new DeviceDiagnosticsDTO();
                dto.setDeviceId(device.getDeviceId());
                dto.setDeviceUid(device.getDeviceUid());
                dto.setDeviceType(device.getDeviceType());
                dto.setDeviceName(device.getDeviceName());
                dto.setStatus(status);
                dto.setOnline(isOnlineStatus(status));
                dto.setFirmwareVersion(
                                firstNonBlank(
                                                healthLog == null
                                                                ? null
                                                                : healthLog.getFirmwareVersion(),
                                                device.getFirmwareVersion()));
                dto.setHardwareVersion(device.getHardwareVersion());
                dto.setLastMqttReceivedAt(lastMqttReceivedAt);
                dto.setLastSeenAt(device.getLastSeenAt());

                if (healthLog != null) {
                        dto.setLatestHealthAt(healthLog.getMeasuredAt());
                        dto.setBatteryPercent(healthLog.getBatteryPercent());
                        dto.setBatteryVoltageV(healthLog.getBatteryVoltageV());
                        dto.setPowerSource(healthLog.getPowerSource());
                        dto.setWifiRssiDbm(healthLog.getWifiRssiDbm());
                        dto.setBleRssiDbm(healthLog.getBleRssiDbm());
                        dto.setFreeHeapBytes(healthLog.getFreeHeapBytes());
                }

                DeviceReplayStatisticsDTO replayStatistics = new DeviceReplayStatisticsDTO();
                replayStatistics.setTotalMqttEvents(
                                rawDeviceEventRepository
                                                .countEventsForDeviceDiagnostics(id));
                replayStatistics.setReplayedEvents(
                                rawDeviceEventRepository
                                                .countReplayedEventsForDeviceDiagnostics(id));
                replayStatistics.setDuplicateEvents(
                                auditService.countDuplicateEventsForDevice(id));
                dto.setReplayStatistics(replayStatistics);

                return dto;
        }

        private Device findDevice(Long id) {
                return deviceRepository
                                .findById(id)
                                .orElseThrow(
                                                () -> new ApiException(
                                                                HttpStatus.NOT_FOUND,
                                                                "DEVICE_NOT_FOUND",
                                                                "Device not found"));
        }

        private BuyerDeviceKitsDTO mapToBuyerDeviceKitsDTO(
                        Buyer buyer,
                        List<DeviceKit> kits,
                        Map<Long, List<DeviceKitDevice>> membershipsByKit,
                        Map<Long, Device> devicesById) {
                if (buyer == null) {
                        return null;
                }

                BuyerDeviceKitsDTO dto = new BuyerDeviceKitsDTO();
                dto.setBuyer(mapToBuyerDTO(buyer));
                dto.setKits(kits.stream()
                                .map(kit -> mapToKitDTO(
                                                kit,
                                                membershipsByKit.getOrDefault(
                                                                kit.getDeviceKitId(),
                                                                List.of()),
                                                devicesById))
                                .toList());
                dto.setPurchaseCount(dto.getKits().size());

                return dto;
        }

        private DeviceKitDTO mapToKitDTO(
                        DeviceKit kit,
                        List<DeviceKitDevice> memberships,
                        Map<Long, Device> devicesById) {
                DeviceKitDTO dto = new DeviceKitDTO();
                dto.setDeviceKitId(kit.getDeviceKitId());
                dto.setKitUid(kit.getKitUid());
                dto.setBuyerId(kit.getBuyerId());
                dto.setPurchaseDate(kit.getPurchaseDate());
                dto.setStatus(kit.getStatus());
                dto.setCreatedAt(kit.getCreatedAt());
                dto.setUpdatedAt(kit.getUpdatedAt());
                dto.setDevices(memberships.stream()
                                .sorted(Comparator.comparingInt(
                                                membership -> kitRoleOrder(
                                                                membership.getKitDeviceRole())))
                                .map(membership -> {
                                        Device device = devicesById.get(membership.getDeviceId());

                                        if (device == null) {
                                                return null;
                                        }

                                        DeviceSummaryDTO summary = mapToSummaryDTO(device);
                                        summary.setKitDeviceRole(membership.getKitDeviceRole());
                                        return summary;
                                })
                                .filter(Objects::nonNull)
                                .toList());

                return dto;
        }

        private DeviceResponseDTO.BuyerDTO mapToBuyerDTO(Buyer buyer) {
                DeviceResponseDTO.BuyerDTO buyerDto = new DeviceResponseDTO.BuyerDTO();
                buyerDto.setFullName(buyer.getFullName());
                buyerDto.setNic(buyer.getNic());
                buyerDto.setContactNumber(buyer.getContactNumber());
                buyerDto.setAddress(buyer.getAddress());
                buyerDto.setPurchaseDate(buyer.getPurchaseDate());
                return buyerDto;
        }

        private int kitRoleOrder(String role) {
                for (int i = 0; i < KIT_DEVICE_DEFINITIONS.size(); i++) {
                        if (KIT_DEVICE_DEFINITIONS.get(i).role().equals(role)) {
                                return i;
                        }
                }

                return KIT_DEVICE_DEFINITIONS.size();
        }

        private PatientDeviceSummaryDTO mapToPatientDeviceSummaryDTO(Device device) {
                Optional<DeviceHealthLog> latestHealth = healthLogRepository
                                .findTopByDeviceIdOrderByMeasuredAtDesc(
                                                device.getDeviceId());
                OffsetDateTime lastMqttReceivedAt = rawDeviceEventRepository
                                .findLatestForDeviceDiagnostics(
                                                device.getDeviceId())
                                .map(RawDeviceEvent::getReceivedAt)
                                .orElse(device.getLastSeenAt());
                DeviceHealthLog healthLog = latestHealth.orElse(null);
                String status = resolveStatus(
                                device,
                                healthLog,
                                lastMqttReceivedAt);

                PatientDeviceSummaryDTO dto = new PatientDeviceSummaryDTO();
                dto.setDeviceId(device.getDeviceId());
                dto.setDeviceUid(device.getDeviceUid());
                dto.setDeviceType(device.getDeviceType());
                dto.setDeviceName(device.getDeviceName());
                dto.setStatus(status);
                dto.setActive(device.getActive());
                dto.setLastSeenAt(device.getLastSeenAt());
                dto.setFirmwareVersion(device.getFirmwareVersion());
                dto.setHardwareVersion(device.getHardwareVersion());
                return dto;
        }

        private DeviceResponseDTO mapToDTO(Device device) {
                Optional<DeviceHealthLog> latestHealth = healthLogRepository
                                .findTopByDeviceIdOrderByMeasuredAtDesc(
                                                device.getDeviceId());
                OffsetDateTime lastMqttReceivedAt = rawDeviceEventRepository
                                .findLatestForDeviceDiagnostics(
                                                device.getDeviceId())
                                .map(RawDeviceEvent::getReceivedAt)
                                .orElse(device.getLastSeenAt());
                DeviceHealthLog healthLog = latestHealth.orElse(null);
                String status = resolveStatus(
                                device,
                                healthLog,
                                lastMqttReceivedAt);

                DeviceResponseDTO dto = new DeviceResponseDTO();

                dto.setDeviceId(device.getDeviceId());
                dto.setPatientId(device.getPatientId());
                dto.setDeviceUid(device.getDeviceUid());
                dto.setAwsThingName(device.getAwsThingName());
                dto.setMqttClientId(device.getMqttClientId());
                dto.setMacAddress(device.getMacAddress());
                dto.setSerialNumber(device.getSerialNumber());
                dto.setDeviceType(device.getDeviceType());
                dto.setDeviceName(device.getDeviceName());
                dto.setCommunicationType(device.getCommunicationType());
                dto.setFirmwareVersion(device.getFirmwareVersion());
                dto.setHardwareVersion(device.getHardwareVersion());
                dto.setStatus(status);
                dto.setOnline(isOnlineStatus(status));
                dto.setBatteryPercent(
                                healthLog == null
                                                ? null
                                                : healthLog.getBatteryPercent());
                dto.setLastSeenAt(device.getLastSeenAt());
                dto.setActive(device.getActive());
                dto.setNotes(device.getNotes());
                dto.setCreatedAt(device.getCreatedAt());
                dto.setUpdatedAt(device.getUpdatedAt());

                if (device.getBuyerId() != null) {
                        buyerRepository.findById(device.getBuyerId()).ifPresent(buyer -> {
                                DeviceResponseDTO.BuyerDTO buyerDto = new DeviceResponseDTO.BuyerDTO();
                                buyerDto.setFullName(buyer.getFullName());
                                buyerDto.setNic(buyer.getNic());
                                buyerDto.setContactNumber(buyer.getContactNumber());
                                buyerDto.setAddress(buyer.getAddress());
                                buyerDto.setPurchaseDate(buyer.getPurchaseDate());
                                dto.setBuyer(buyerDto);
                        });
                }

                if (device.getPatientId() != null) {
                        patientRepository.findById(device.getPatientId()).ifPresent(patient -> {
                                dto.setPatientDisplayName(patient.getFullName());
                        });
                }

                return dto;
        }

        private DeviceSummaryDTO mapToSummaryDTO(Device device) {
                Optional<DeviceHealthLog> latestHealth = healthLogRepository
                                .findTopByDeviceIdOrderByMeasuredAtDesc(
                                                device.getDeviceId());
                OffsetDateTime lastMqttReceivedAt = rawDeviceEventRepository
                                .findLatestForDeviceDiagnostics(
                                                device.getDeviceId())
                                .map(RawDeviceEvent::getReceivedAt)
                                .orElse(device.getLastSeenAt());
                DeviceHealthLog healthLog = latestHealth.orElse(null);
                String status = resolveStatus(
                                device,
                                healthLog,
                                lastMqttReceivedAt);

                DeviceSummaryDTO dto = new DeviceSummaryDTO();
                dto.setDeviceId(device.getDeviceId());
                dto.setPatientId(device.getPatientId());
                dto.setDeviceUid(device.getDeviceUid());
                dto.setDeviceType(device.getDeviceType());
                dto.setDeviceName(device.getDeviceName());
                dto.setStatus(status);
                dto.setOnline(isOnlineStatus(status));
                dto.setBatteryPercent(
                                healthLog == null
                                                ? null
                                                : healthLog.getBatteryPercent());
                dto.setLastSeenAt(device.getLastSeenAt());
                dto.setActive(device.getActive());

                if (device.getBuyerId() != null) {
                        buyerRepository.findById(device.getBuyerId()).ifPresent(buyer -> {
                                DeviceResponseDTO.BuyerDTO buyerDto = new DeviceResponseDTO.BuyerDTO();
                                buyerDto.setFullName(buyer.getFullName());
                                buyerDto.setNic(buyer.getNic());
                                buyerDto.setContactNumber(buyer.getContactNumber());
                                buyerDto.setAddress(buyer.getAddress());
                                buyerDto.setPurchaseDate(buyer.getPurchaseDate());
                                dto.setBuyer(buyerDto);
                        });
                }

                if (device.getPatientId() != null) {
                        patientRepository.findById(device.getPatientId()).ifPresent(patient -> {
                                dto.setPatientDisplayName(patient.getFullName());
                        });
                }

                return dto;
        }

        private String resolveStatus(
                        Device device,
                        DeviceHealthLog healthLog,
                        OffsetDateTime lastMqttReceivedAt) {
                if (!Boolean.TRUE.equals(device.getActive())) {
                        return "DEACTIVATED";
                }

                if (healthLog != null
                                && (Boolean.FALSE.equals(healthLog.getOnline())
                                                || "OFFLINE".equals(healthLog.getStatus()))) {
                        return "OFFLINE";
                }

                if (lastMqttReceivedAt == null) {
                        return healthLog == null
                                        ? "UNKNOWN"
                                        : defaultStatus(healthLog.getStatus());
                }

                if (lastMqttReceivedAt.isBefore(
                                OffsetDateTime
                                                .now(ZoneOffset.UTC)
                                                .minusMinutes(OFFLINE_AFTER_MINUTES))) {
                        return "OFFLINE";
                }

                return healthLog == null
                                ? "ONLINE"
                                : defaultStatus(healthLog.getStatus());
        }

        private boolean isOnlineStatus(String status) {
                return "ONLINE".equals(status)
                                || "LOW_BATTERY".equals(status);
        }

        private String defaultStatus(String status) {
                String normalized = trimToNull(status);
                return normalized == null || "UNKNOWN".equals(normalized)
                                ? "ONLINE"
                                : normalized;
        }

        private void assertUniqueOptional(
                        String fieldName,
                        String value,
                        UniqueLookup lookup) {
                String normalized = trimToNull(value);

                if (normalized == null) {
                        return;
                }

                if (lookup.find(normalized).isPresent()) {
                        throw new ApiException(
                                        HttpStatus.CONFLICT,
                                        "DEVICE_IDENTIFIER_EXISTS",
                                        fieldName + " is already used by another device");
                }
        }

        private String normalizeRequired(
                        String value,
                        Set<String> allowedValues,
                        String fieldName) {
                String normalized = normalize(value);

                if (normalized == null || !allowedValues.contains(normalized)) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "VALIDATION_ERROR",
                                        fieldName + " is invalid");
                }

                return normalized;
        }

        private String normalizeOptional(
                        String value,
                        Set<String> allowedValues,
                        String defaultValue) {
                String normalized = normalize(value);

                if (normalized == null) {
                        return defaultValue;
                }

                return allowedValues.contains(normalized)
                                ? normalized
                                : defaultValue;
        }

        private String normalize(String value) {
                String trimmed = trimToNull(value);

                if (trimmed == null) {
                        return null;
                }

                String normalized = trimmed
                                .toUpperCase(Locale.ROOT)
                                .replace('-', '_')
                                .replace(' ', '_');

                if ("OUTER_UNIT".equals(normalized)) {
                        return "OUTER_GATEWAY";
                }

                return normalized;
        }

        private String requireText(
                        String value,
                        String fieldName) {
                String trimmed = trimToNull(value);

                if (trimmed == null) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "VALIDATION_ERROR",
                                        fieldName + " is required");
                }

                return trimmed;
        }

        private String firstNonBlank(String first, String second) {
                String firstValue = trimToNull(first);
                return firstValue == null ? trimToNull(second) : firstValue;
        }

        private String trimToNull(String value) {
                if (value == null) {
                        return null;
                }

                String trimmed = value.trim();
                return trimmed.isEmpty() ? null : trimmed;
        }

        private record KitDeviceDefinition(
                        String role,
                        String deviceType,
                        String deviceName,
                        String communicationType) {
        }

        @FunctionalInterface
        private interface UniqueLookup {
                Optional<Device> find(String value);
        }
}
