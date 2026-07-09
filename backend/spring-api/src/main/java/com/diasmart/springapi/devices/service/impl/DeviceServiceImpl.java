package com.diasmart.springapi.devices.service.impl;

import com.diasmart.springapi.audit.service.AuditService;
import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.devices.dto.AssignDeviceRequestDTO;
import com.diasmart.springapi.devices.dto.DeviceDiagnosticsDTO;
import com.diasmart.springapi.devices.dto.DeviceReplayStatisticsDTO;
import com.diasmart.springapi.devices.dto.DeviceResponseDTO;
import com.diasmart.springapi.devices.dto.DeviceSummaryDTO;
import com.diasmart.springapi.devices.dto.RegisterDeviceRequestDTO;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.entity.DeviceHealthLog;
import com.diasmart.springapi.devices.repository.DeviceHealthLogRepository;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.devices.service.DeviceService;
import com.diasmart.springapi.raw_events.entity.RawDeviceEvent;
import com.diasmart.springapi.raw_events.repository.RawDeviceEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.LocalDate;
import com.diasmart.springapi.devices.dto.BuyerDeviceKitsDTO;
import com.diasmart.springapi.devices.dto.DeviceKitDTO;
import com.diasmart.springapi.devices.dto.DeviceKitRegistrationRequestDTO;
import com.diasmart.springapi.devices.entity.Buyer;
import com.diasmart.springapi.devices.entity.DeviceStatus;
import com.diasmart.springapi.devices.repository.BuyerRepository;
import com.diasmart.springapi.patients.repository.PatientRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

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

        private final DeviceRepository deviceRepository;
        private final DeviceHealthLogRepository healthLogRepository;
        private final RawDeviceEventRepository rawDeviceEventRepository;
        private final AuditService auditService;
        private final BuyerRepository buyerRepository;
        private final PatientRepository patientRepository;

        public DeviceServiceImpl(
                        DeviceRepository deviceRepository,
                        DeviceHealthLogRepository healthLogRepository,
                        RawDeviceEventRepository rawDeviceEventRepository,
                        AuditService auditService,
                        BuyerRepository buyerRepository,
                        PatientRepository patientRepository) {
                this.deviceRepository = deviceRepository;
                this.healthLogRepository = healthLogRepository;
                this.rawDeviceEventRepository = rawDeviceEventRepository;
                this.auditService = auditService;
                this.buyerRepository = buyerRepository;
                this.patientRepository = patientRepository;
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
        public List<BuyerDeviceKitsDTO> getDeviceKits() {
                List<Device> allDevices = deviceRepository.findAllByOrderByDeviceIdAsc();
                List<Buyer> allBuyers = buyerRepository.findAll();

                return allBuyers.stream().map(buyer -> {
                        BuyerDeviceKitsDTO dto = new BuyerDeviceKitsDTO();
                        
                        DeviceResponseDTO.BuyerDTO buyerDto = new DeviceResponseDTO.BuyerDTO();
                        buyerDto.setFullName(buyer.getFullName());
                        buyerDto.setNic(buyer.getNic());
                        buyerDto.setContactNumber(buyer.getContactNumber());
                        buyerDto.setAddress(buyer.getAddress());
                        buyerDto.setPurchaseDate(buyer.getPurchaseDate());
                        dto.setBuyer(buyerDto);

                        List<Device> buyerDevices = allDevices.stream()
                                .filter(d -> buyer.getBuyerId().equals(d.getBuyerId()))
                                .toList();

                        java.util.Map<LocalDate, List<Device>> groupedByDate = buyerDevices.stream()
                                .collect(java.util.stream.Collectors.groupingBy(d -> 
                                        d.getCreatedAt() != null ? d.getCreatedAt().toLocalDate() : buyer.getPurchaseDate()
                                ));

                        List<DeviceKitDTO> kits = groupedByDate.entrySet().stream()
                                .sorted((e1, e2) -> e2.getKey().compareTo(e1.getKey())) // Sort descending by date
                                .map(entry -> {
                                        DeviceKitDTO kitDto = new DeviceKitDTO();
                                        kitDto.setPurchaseDate(entry.getKey());
                                        kitDto.setDevices(entry.getValue().stream().map(this::mapToSummaryDTO).toList());
                                        return kitDto;
                                })
                                .toList();

                        dto.setKits(kits);
                        dto.setPurchaseCount(kits.size());

                        return dto;
                }).filter(dto -> dto.getPurchaseCount() > 0).toList();
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
        public void registerDeviceKit(DeviceKitRegistrationRequestDTO dto) {
                // Ensure at least one ID is provided
                boolean hasOuter = dto.getOuterGatewayId() != null && !dto.getOuterGatewayId().trim().isEmpty();
                boolean hasInner = dto.getInnerUnitId() != null && !dto.getInnerUnitId().trim().isEmpty();
                boolean hasPen = dto.getPenUnitId() != null && !dto.getPenUnitId().trim().isEmpty();
                boolean hasGluco = dto.getGlucoseMeterId() != null && !dto.getGlucoseMeterId().trim().isEmpty();

                if (!hasOuter && !hasInner && !hasPen && !hasGluco) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "NO_DEVICES_PROVIDED", "At least one Device ID must be provided.");
                }

                // Check if any provided device ID already exists
                checkDeviceExists(dto.getOuterGatewayId(), "Outer Gateway");
                checkDeviceExists(dto.getInnerUnitId(), "Inner Unit");
                checkDeviceExists(dto.getPenUnitId(), "Pen Unit");
                checkDeviceExists(dto.getGlucoseMeterId(), "Glucose Meter");

                // Get or Create the Buyer
                buyerRepository.findByNic(dto.getNic()).ifPresent(b -> {
                        throw new ApiException(HttpStatus.CONFLICT, "BUYER_ALREADY_REGISTERED", "A device kit is already registered under this NIC/Passport.");
                });

                Buyer buyer = new Buyer();
                buyer.setFullName(dto.getBuyerFullName());
                buyer.setNic(dto.getNic());
                buyer.setContactNumber(dto.getContactNumber());
                buyer.setAddress(dto.getAddress());
                buyer.setPurchaseDate(dto.getPurchaseDate());
                buyer = buyerRepository.save(buyer);

                // Create the Devices
                if (hasOuter) createDevice(dto.getOuterGatewayId(), "OUTER_GATEWAY", "DiaSmart Outer Gateway", "MQTTS", buyer.getBuyerId());
                if (hasInner) createDevice(dto.getInnerUnitId(), "INNER_UNIT", "DiaSmart Inner Unit", "ESP_NOW", buyer.getBuyerId());
                if (hasPen) createDevice(dto.getPenUnitId(), "DOSE_CAP", "DiaSmart Pen Unit", "BLE", buyer.getBuyerId());
                if (hasGluco) createDevice(dto.getGlucoseMeterId(), "GLUCOMETER", "DiaSmart Glucose Meter", "BLE", buyer.getBuyerId());
        }

        private void checkDeviceExists(String uid, String label) {
                if (uid != null && !uid.trim().isEmpty()) {
                        if (deviceRepository.existsByDeviceUid(uid)) {
                                throw new ApiException(HttpStatus.CONFLICT, "DEVICE_ALREADY_EXISTS",
                                                label + " with ID " + uid + " already exists.");
                        }
                }
        }

        private void createDevice(String uid, String type, String name, String commType, Long buyerId) {
                Device d = new Device();
                d.setDeviceUid(uid);
                d.setDeviceType(type);
                d.setDeviceName(name);
                d.setCommunicationType(commType);
                d.setBuyerId(buyerId);
                d.setPatientId(null);
                d.setActive(true);
                d.setStatus(DeviceStatus.UNKNOWN);
                deviceRepository.save(d);
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

        @FunctionalInterface
        private interface UniqueLookup {
                Optional<Device> find(String value);
        }
}
