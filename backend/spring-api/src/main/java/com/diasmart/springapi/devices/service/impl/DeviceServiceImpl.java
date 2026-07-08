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
import com.diasmart.springapi.devices.dto.DeviceKitRegistrationRequestDTO;
import com.diasmart.springapi.devices.entity.Buyer;
import com.diasmart.springapi.devices.entity.DeviceStatus;
import com.diasmart.springapi.devices.repository.BuyerRepository;
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

        public DeviceServiceImpl(
                        DeviceRepository deviceRepository,
                        DeviceHealthLogRepository healthLogRepository,
                        RawDeviceEventRepository rawDeviceEventRepository,
                        AuditService auditService,
                        BuyerRepository buyerRepository) {
                this.deviceRepository = deviceRepository;
                this.healthLogRepository = healthLogRepository;
                this.rawDeviceEventRepository = rawDeviceEventRepository;
                this.auditService = auditService;
                this.buyerRepository = buyerRepository;
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
                List<Device> kitDevices = new java.util.ArrayList<>();
                if (dto.getOuterGatewayId() != null && !dto.getOuterGatewayId().trim().isEmpty()) {
                        kitDevices.add(findDeviceByUid(dto.getOuterGatewayId(), "Outer Gateway"));
                }
                if (dto.getInnerUnitId() != null && !dto.getInnerUnitId().trim().isEmpty()) {
                        kitDevices.add(findDeviceByUid(dto.getInnerUnitId(), "Inner Unit"));
                }
                if (dto.getPenUnitId() != null && !dto.getPenUnitId().trim().isEmpty()) {
                        kitDevices.add(findDeviceByUid(dto.getPenUnitId(), "Pen Unit"));
                }
                if (dto.getGlucoseMeterId() != null && !dto.getGlucoseMeterId().trim().isEmpty()) {
                        kitDevices.add(findDeviceByUid(dto.getGlucoseMeterId(), "Glucose Meter"));
                }

                if (kitDevices.isEmpty()) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "NO_DEVICES_PROVIDED", "At least one Device ID must be provided.");
                }

                for (Device d : kitDevices) {
                        if (d.getBuyerId() != null || (d.getStatus() != null && d.getStatus() != DeviceStatus.NEW)) {
                                throw new ApiException(HttpStatus.CONFLICT, "DEVICE_ALREADY_REGISTERED", "Device " + d.getDeviceUid() + " is already registered.");
                        }
                }

                Buyer buyer = new Buyer();
                buyer.setFullName(dto.getBuyerFullName());
                buyer.setNic(dto.getNic());
                buyer.setContactNumber(dto.getContactNumber());
                buyer.setAddress(dto.getAddress());
                buyer.setPurchaseDate(dto.getPurchaseDate());

                buyer = buyerRepository.save(buyer);

                for (Device d : kitDevices) {
                        d.setBuyerId(buyer.getBuyerId());
                        d.setStatus(DeviceStatus.AVAILABLE);
                        d.setActive(true);
                        deviceRepository.save(d);
                }
        }

        private Device findDeviceByUid(String uid, String label) {
                return deviceRepository.findByDeviceUid(uid)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DEVICE_NOT_FOUND", label + " ID is invalid or does not exist."));
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
