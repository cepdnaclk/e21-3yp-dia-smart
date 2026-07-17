package com.diasmart.springapi.careplan.service;

import com.diasmart.springapi.careplan.dto.CarePlanResponse;
import com.diasmart.springapi.careplan.entity.CarePlanSchedule;
import com.diasmart.springapi.careplan.entity.CarePlanSnapshot;
import com.diasmart.springapi.careplan.repository.CarePlanScheduleRepository;
import com.diasmart.springapi.careplan.repository.CarePlanSnapshotRepository;
import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.dose_schedules.entity.DoseSchedule;
import com.diasmart.springapi.dose_schedules.repository.DoseScheduleRepository;
import com.diasmart.springapi.prescriptions.entity.Prescription;
import com.diasmart.springapi.prescriptions.repository.PrescriptionRepository;
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.exceptions.ResourceNotFoundException;
import com.diasmart.springapi.shared.security.AuthorizationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CarePlanService {

    private static final String OUTER_GATEWAY = "OUTER_GATEWAY";
    private static final String DEFAULT_TIMEZONE = "Asia/Colombo";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final CarePlanSnapshotRepository snapshotRepository;
    private final CarePlanScheduleRepository carePlanScheduleRepository;
    private final DoseScheduleRepository doseScheduleRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final DeviceRepository deviceRepository;
    private final AuthorizationService authorizationService;
    private final CarePlanPublisherService publisherService;
    private final ObjectMapper objectMapper;

    public CarePlanService(
            CarePlanSnapshotRepository snapshotRepository,
            CarePlanScheduleRepository carePlanScheduleRepository,
            DoseScheduleRepository doseScheduleRepository,
            PrescriptionRepository prescriptionRepository,
            DeviceRepository deviceRepository,
            AuthorizationService authorizationService,
            CarePlanPublisherService publisherService,
            ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.carePlanScheduleRepository = carePlanScheduleRepository;
        this.doseScheduleRepository = doseScheduleRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.deviceRepository = deviceRepository;
        this.authorizationService = authorizationService;
        this.publisherService = publisherService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CarePlanResponse generateAndPublish(Long patientId) {
        authorizationService.authorize(Permission.MANAGE_SCHEDULE, patientId);
        return toResponse(generateAndPublishInternal(patientId, DEFAULT_TIMEZONE, LocalDate.now()));
    }

    @Transactional(readOnly = true)
    public CarePlanResponse getCurrent(Long patientId) {
        authorizationService.authorize(Permission.READ_SCHEDULE, patientId);

        CarePlanSnapshot snapshot = snapshotRepository.findTopByPatientIdOrderByVersionDesc(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Care Plan not found for patient: " + patientId));

        return toResponse(snapshot);
    }

    @Transactional
    public CarePlanResponse resendCurrent(Long patientId) {
        authorizationService.authorize(Permission.MANAGE_SCHEDULE, patientId);

        CarePlanSnapshot snapshot = snapshotRepository.findTopByPatientIdOrderByVersionDesc(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Care Plan not found for patient: " + patientId));

        snapshot.setStatus("PENDING");
        snapshot = snapshotRepository.save(snapshot);
        return toResponse(publisherService.publish(snapshot));
    }

    @Transactional
    public void resendCurrentForDevice(Device outerDevice) {
        if (outerDevice == null || outerDevice.getPatientId() == null) {
            return;
        }

        snapshotRepository.findTopByPatientIdOrderByVersionDesc(outerDevice.getPatientId())
                .filter(snapshot -> outerDevice.getDeviceId().equals(snapshot.getOuterDeviceId()))
                .ifPresent(snapshot -> {
                    snapshot.setStatus("PENDING");
                    publisherService.publish(snapshotRepository.save(snapshot));
                });
    }

    public void regenerateAfterPrescriptionChange(Long patientId) {
        try {
            generateAndPublishInternal(patientId, DEFAULT_TIMEZONE, LocalDate.now());
        } catch (RuntimeException ex) {
            System.out.println("Care Plan regeneration skipped: " + ex.getMessage());
        }
    }

    private CarePlanSnapshot generateAndPublishInternal(Long patientId, String timezone, LocalDate effectiveFrom) {
        Device outerDevice = deviceRepository.findFirstByPatientIdAndDeviceTypeAndActiveTrue(patientId, OUTER_GATEWAY)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "OUTER_DEVICE_NOT_FOUND", "Active Outer Unit not found for patient"));

        List<DoseSchedule> schedules = doseScheduleRepository.findByPatientIdAndActiveTrue(patientId);
        int version = snapshotRepository.findTopByPatientIdOrderByVersionDesc(patientId)
                .map(snapshot -> snapshot.getVersion() + 1)
                .orElse(1);

        CarePlanSnapshot snapshot = new CarePlanSnapshot();
        snapshot.setPatientId(patientId);
        snapshot.setOuterDeviceId(outerDevice.getDeviceId());
        snapshot.setOuterDeviceUid(outerDevice.getDeviceUid());
        snapshot.setVersion(version);
        snapshot.setCarePlanUid("CP-" + patientId + "-" + version);
        snapshot.setTimezone(timezone);
        snapshot.setEffectiveFrom(effectiveFrom);
        snapshot.setStatus("PENDING");
        snapshot.setPayload("{}");
        snapshot = snapshotRepository.save(snapshot);

        List<Map<String, Object>> schedulePayloads = new ArrayList<>();
        for (DoseSchedule schedule : schedules) {
            CarePlanSchedule carePlanSchedule = toCarePlanSchedule(snapshot.getSnapshotId(), schedule);
            carePlanSchedule = carePlanScheduleRepository.save(carePlanSchedule);
            schedulePayloads.add(toSchedulePayload(carePlanSchedule));
        }

        snapshot.setPayload(buildPayload(snapshot, schedulePayloads));
        snapshot = snapshotRepository.save(snapshot);
        return publisherService.publish(snapshot);
    }

    private CarePlanSchedule toCarePlanSchedule(Long snapshotId, DoseSchedule schedule) {
        CarePlanSchedule carePlanSchedule = new CarePlanSchedule();
        carePlanSchedule.setSnapshotId(snapshotId);
        carePlanSchedule.setSourceScheduleId(schedule.getScheduleId());
        carePlanSchedule.setScheduleExternalId("SCH-" + schedule.getScheduleId());
        carePlanSchedule.setPeriod(resolvePeriod(schedule));
        carePlanSchedule.setInsulinType(resolveInsulinType(schedule.getPrescriptionId()));
        carePlanSchedule.setDoseUnits(schedule.getDoseUnits());
        carePlanSchedule.setTargetTime(resolveTargetTime(schedule));
        carePlanSchedule.setWindowStart(resolveWindowStart(schedule));
        carePlanSchedule.setWindowEnd(resolveWindowEnd(schedule));
        return carePlanSchedule;
    }

    private Map<String, Object> toSchedulePayload(CarePlanSchedule schedule) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("scheduleId", schedule.getScheduleExternalId());
        payload.put("period", schedule.getPeriod());
        payload.put("insulinType", schedule.getInsulinType());
        payload.put("doseUnits", schedule.getDoseUnits());
        payload.put("windowStart", formatTime(schedule.getWindowStart()));
        payload.put("targetTime", formatTime(schedule.getTargetTime()));
        payload.put("windowEnd", formatTime(schedule.getWindowEnd()));
        return payload;
    }

    private String buildPayload(CarePlanSnapshot snapshot, List<Map<String, Object>> schedules) {
        Map<String, Object> reminderSettings = new HashMap<>();
        reminderSettings.put("buzzerDurationMinutes", 3);
        reminderSettings.put("repeatIntervalMinutes", 15);
        reminderSettings.put("manualStopAllowed", true);

        Map<String, Object> payload = new HashMap<>();
        payload.put("carePlanId", snapshot.getCarePlanUid());
        payload.put("version", snapshot.getVersion());
        payload.put("patientId", "PATIENT-" + snapshot.getPatientId());
        payload.put("outerDeviceId", snapshot.getOuterDeviceUid());
        payload.put("timezone", snapshot.getTimezone());
        payload.put("effectiveFrom", snapshot.getEffectiveFrom().toString());
        payload.put("schedules", schedules);
        payload.put("reminderSettings", reminderSettings);

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CARE_PLAN_SERIALIZATION_ERROR", "Failed to serialize Care Plan");
        }
    }

    private CarePlanResponse toResponse(CarePlanSnapshot snapshot) {
        CarePlanResponse response = new CarePlanResponse();
        response.setSnapshotId(snapshot.getSnapshotId());
        response.setCarePlanId(snapshot.getCarePlanUid());
        response.setVersion(snapshot.getVersion());
        response.setPatientId(snapshot.getPatientId());
        response.setOuterDeviceId(snapshot.getOuterDeviceId());
        response.setOuterDeviceUid(snapshot.getOuterDeviceUid());
        response.setTimezone(snapshot.getTimezone());
        response.setEffectiveFrom(snapshot.getEffectiveFrom());
        response.setStatus(snapshot.getStatus());
        response.setPublishedAt(snapshot.getPublishedAt());
        response.setAcknowledgedAt(snapshot.getAcknowledgedAt());
        response.setPayload(parsePayload(snapshot.getPayload()));
        return response;
    }

    private Map<String, Object> parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    private LocalTime resolveTargetTime(DoseSchedule schedule) {
        if (schedule.getTargetTime() != null) {
            return schedule.getTargetTime();
        }

        return schedule.getScheduledTime();
    }

    private LocalTime resolveWindowStart(DoseSchedule schedule) {
        if (schedule.getWindowStart() != null) {
            return schedule.getWindowStart();
        }

        return resolveTargetTime(schedule).minusMinutes(defaultInteger(schedule.getAllowedEarlyMinutes(), 60));
    }

    private LocalTime resolveWindowEnd(DoseSchedule schedule) {
        if (schedule.getWindowEnd() != null) {
            return schedule.getWindowEnd();
        }

        return resolveTargetTime(schedule).plusMinutes(defaultInteger(schedule.getAllowedLateMinutes(), 120));
    }

    private Integer defaultInteger(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String resolveInsulinType(Long prescriptionId) {
        return prescriptionRepository.findById(prescriptionId)
                .map(Prescription::getPrescriptionName)
                .filter(value -> !value.isBlank())
                .orElse("Insulin");
    }

    private String resolvePeriod(DoseSchedule schedule) {
        String label = schedule.getScheduleLabel();
        if (label != null) {
            String normalized = label.toUpperCase(Locale.ROOT);
            if (normalized.contains("MORNING")) {
                return "MORNING";
            }
            if (normalized.contains("AFTERNOON")) {
                return "AFTERNOON";
            }
            if (normalized.contains("EVENING")) {
                return "EVENING";
            }
            if (normalized.contains("NIGHT")) {
                return "NIGHT";
            }
        }

        int hour = resolveTargetTime(schedule).getHour();
        if (hour < 12) {
            return "MORNING";
        }
        if (hour < 17) {
            return "AFTERNOON";
        }
        if (hour < 21) {
            return "EVENING";
        }
        return "NIGHT";
    }

    private String formatTime(LocalTime time) {
        return time.format(TIME_FORMATTER);
    }
}
