package com.diasmart.springapi.careplan.service;

import com.diasmart.springapi.careplan.entity.CarePlanSchedule;
import com.diasmart.springapi.careplan.entity.CarePlanSnapshot;
import com.diasmart.springapi.careplan.repository.CarePlanScheduleRepository;
import com.diasmart.springapi.careplan.repository.CarePlanSnapshotRepository;
import com.diasmart.springapi.dose_schedules.entity.DoseSchedule;
import com.diasmart.springapi.dose_schedules.repository.DoseScheduleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DoseScheduleMatchingService {

    private final CarePlanSnapshotRepository snapshotRepository;
    private final CarePlanScheduleRepository carePlanScheduleRepository;
    private final DoseScheduleRepository doseScheduleRepository;

    public DoseScheduleMatchingService(
            CarePlanSnapshotRepository snapshotRepository,
            CarePlanScheduleRepository carePlanScheduleRepository,
            DoseScheduleRepository doseScheduleRepository) {
        this.snapshotRepository = snapshotRepository;
        this.carePlanScheduleRepository = carePlanScheduleRepository;
        this.doseScheduleRepository = doseScheduleRepository;
    }

    public Optional<MatchResult> match(
            Long patientId,
            Integer carePlanVersion,
            String scheduleExternalId,
            OffsetDateTime eventTimestamp,
            BigDecimal doseUnits
    ) {
        Optional<CarePlanSnapshot> snapshot = carePlanVersion == null
                ? snapshotRepository.findTopByPatientIdOrderByVersionDesc(patientId)
                : snapshotRepository.findByPatientIdAndVersion(patientId, carePlanVersion);

        if (snapshot.isEmpty()) {
            return Optional.empty();
        }

        if (scheduleExternalId != null && !scheduleExternalId.isBlank()) {
            Optional<CarePlanSchedule> suppliedSchedule = carePlanScheduleRepository
                    .findBySnapshotIdAndScheduleExternalId(snapshot.get().getSnapshotId(), scheduleExternalId);

            if (suppliedSchedule.isPresent()) {
                return toMatchResult(suppliedSchedule.get());
            }
        }

        LocalTime eventTime = eventTimestamp.toLocalTime();
        List<CarePlanSchedule> matches = carePlanScheduleRepository.findBySnapshotId(snapshot.get().getSnapshotId())
                .stream()
                .filter(schedule -> isWithinWindow(eventTime, schedule.getWindowStart(), schedule.getWindowEnd()))
                .filter(schedule -> doseUnits == null || schedule.getDoseUnits().compareTo(doseUnits) == 0)
                .toList();

        if (matches.size() != 1) {
            return Optional.empty();
        }

        return toMatchResult(matches.get(0));
    }

    private Optional<MatchResult> toMatchResult(CarePlanSchedule schedule) {
        Optional<DoseSchedule> sourceSchedule = doseScheduleRepository.findById(schedule.getSourceScheduleId());

        return sourceSchedule.map(doseSchedule -> new MatchResult(
                doseSchedule.getScheduleId(),
                doseSchedule.getPrescriptionId(),
                schedule.getScheduleExternalId()
        ));
    }

    private boolean isWithinWindow(LocalTime value, LocalTime start, LocalTime end) {
        return !value.isBefore(start) && !value.isAfter(end);
    }

    public static class MatchResult {

        private final Long scheduleId;
        private final Long prescriptionId;
        private final String scheduleExternalId;

        public MatchResult(Long scheduleId, Long prescriptionId, String scheduleExternalId) {
            this.scheduleId = scheduleId;
            this.prescriptionId = prescriptionId;
            this.scheduleExternalId = scheduleExternalId;
        }

        public Long getScheduleId() {
            return scheduleId;
        }

        public Long getPrescriptionId() {
            return prescriptionId;
        }

        public String getScheduleExternalId() {
            return scheduleExternalId;
        }
    }
}
