package com.diasmart.springapi.dose_schedules.service;

import com.diasmart.springapi.dose_schedules.dto.DoseScheduleResponse;
import com.diasmart.springapi.dose_schedules.entity.DoseSchedule;
import com.diasmart.springapi.dose_schedules.repository.DoseScheduleRepository;
import com.diasmart.springapi.careplan.service.CarePlanService;
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.diasmart.springapi.dose_schedules.dto.CreateDoseScheduleRequest;

import java.time.LocalTime;
import java.time.OffsetDateTime;

import com.diasmart.springapi.dose_schedules.dto.UpdateDoseScheduleRequest;
import com.diasmart.springapi.shared.exceptions.ResourceNotFoundException;

import com.diasmart.springapi.dose.entity.DoseEvent;
import com.diasmart.springapi.dose.repository.DoseEventRepository;
import com.diasmart.springapi.dose_schedules.dto.ScheduleAdherenceResponse;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class DoseScheduleService {

    private final DoseScheduleRepository doseScheduleRepository;

    private final AuthorizationService authorizationService;

    private final DoseEventRepository doseEventRepository;

    private final CarePlanService carePlanService;

    public DoseScheduleService(
            DoseScheduleRepository doseScheduleRepository,
            AuthorizationService authorizationService,
            DoseEventRepository doseEventRepository,
            CarePlanService carePlanService
    ) {
        this.doseScheduleRepository = doseScheduleRepository;
        this.authorizationService = authorizationService;
        this.doseEventRepository = doseEventRepository;
        this.carePlanService = carePlanService;
    }

    public Page<DoseScheduleResponse> getDoseSchedules(
            Long patientId,
            Pageable pageable
    ) {

        authorizationService.authorize(
                Permission.READ_PATIENT_READINGS,
                patientId
        );

        return doseScheduleRepository
                .findByPatientIdAndActiveTrue(
                        patientId,
                        pageable
                )
                .map(this::mapToResponse);
    }

    private DoseScheduleResponse mapToResponse(
            DoseSchedule doseSchedule
    ) {

        DoseScheduleResponse response =
                new DoseScheduleResponse();

        response.setScheduleId(
                doseSchedule.getScheduleId()
        );

        response.setPrescriptionId(
                doseSchedule.getPrescriptionId()
        );

        response.setScheduleLabel(
                doseSchedule.getScheduleLabel()
        );

        response.setScheduledTime(
                doseSchedule.getScheduledTime()
        );

        response.setWindowStart(
                resolveWindowStart(doseSchedule)
        );

        response.setTargetTime(
                resolveTargetTime(doseSchedule)
        );

        response.setWindowEnd(
                resolveWindowEnd(doseSchedule)
        );

        response.setDoseUnits(
                doseSchedule.getDoseUnits()
        );

        response.setDaysOfWeek(
                doseSchedule.getDaysOfWeek()
        );

        response.setAllowedEarlyMinutes(
                doseSchedule.getAllowedEarlyMinutes()
        );

        response.setAllowedLateMinutes(
                doseSchedule.getAllowedLateMinutes()
        );

        response.setActive(
                doseSchedule.getActive()
        );

        response.setCreatedAt(
                doseSchedule.getCreatedAt()
        );

        return response;
    }

    public DoseScheduleResponse createDoseSchedule(
        Long patientId,
        CreateDoseScheduleRequest request
        ) {

    authorizationService.authorize(
            Permission.WRITE_MANUAL_DOSE,
            patientId
    );

    DoseSchedule doseSchedule =
            new DoseSchedule();

    doseSchedule.setPrescriptionId(
            request.getPrescriptionId()
    );

    doseSchedule.setPatientId(patientId);

    doseSchedule.setScheduleLabel(
            request.getScheduleLabel()
    );

    LocalTime targetTime = parseTime(firstNonBlank(request.getTargetTime(), request.getScheduledTime()));
    Integer allowedEarlyMinutes = defaultInteger(request.getAllowedEarlyMinutes(), 60);
    Integer allowedLateMinutes = defaultInteger(request.getAllowedLateMinutes(), 120);

    doseSchedule.setScheduledTime(targetTime);
    doseSchedule.setTargetTime(targetTime);
    doseSchedule.setWindowStart(parseTimeOrDefault(request.getWindowStart(), targetTime.minusMinutes(allowedEarlyMinutes)));
    doseSchedule.setWindowEnd(parseTimeOrDefault(request.getWindowEnd(), targetTime.plusMinutes(allowedLateMinutes)));
    validateWindow(doseSchedule.getWindowStart(), doseSchedule.getTargetTime(), doseSchedule.getWindowEnd());

    doseSchedule.setDoseUnits(
            request.getDoseUnits()
    );

    doseSchedule.setDaysOfWeek(
            firstNonBlank(request.getDaysOfWeek(), "1,2,3,4,5,6,7")
    );

    doseSchedule.setAllowedEarlyMinutes(
            allowedEarlyMinutes
    );

    doseSchedule.setAllowedLateMinutes(
            allowedLateMinutes
    );

    doseSchedule.setActive(true);

    doseSchedule.setCreatedAt(
            OffsetDateTime.now()
    );

    doseSchedule.setUpdatedAt(
            OffsetDateTime.now()
    );

    DoseSchedule savedDoseSchedule =
            doseScheduleRepository.save(
                    doseSchedule
            );

    carePlanService.regenerateAfterPrescriptionChange(patientId);

    return mapToResponse(
            savedDoseSchedule
    );
    }

    public DoseScheduleResponse updateDoseSchedule(
        Long scheduleId,
        UpdateDoseScheduleRequest request
) {

    DoseSchedule doseSchedule =
            doseScheduleRepository.findById(
                    scheduleId
            ).orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Dose schedule not found with id: "
                                    + scheduleId
                    )
            );

    authorizationService.authorize(
            Permission.WRITE_MANUAL_DOSE,
            doseSchedule.getPatientId()
    );

    if (request.getScheduleLabel() != null) {

        doseSchedule.setScheduleLabel(
                request.getScheduleLabel()
        );
    }

    if (request.getScheduledTime() != null) {

        LocalTime scheduledTime = LocalTime.parse(request.getScheduledTime());
        doseSchedule.setScheduledTime(scheduledTime);
        if (doseSchedule.getTargetTime() == null) {
            doseSchedule.setTargetTime(scheduledTime);
        }
    }

    if (request.getTargetTime() != null) {
        LocalTime targetTime = LocalTime.parse(request.getTargetTime());
        doseSchedule.setTargetTime(targetTime);
        doseSchedule.setScheduledTime(targetTime);
    }

    if (request.getWindowStart() != null) {
        doseSchedule.setWindowStart(LocalTime.parse(request.getWindowStart()));
    }

    if (request.getWindowEnd() != null) {
        doseSchedule.setWindowEnd(LocalTime.parse(request.getWindowEnd()));
    }

    if (request.getDoseUnits() != null) {

        doseSchedule.setDoseUnits(
                request.getDoseUnits()
        );
    }

    if (request.getDaysOfWeek() != null) {

        doseSchedule.setDaysOfWeek(
                request.getDaysOfWeek()
        );
    }

    if (request.getAllowedEarlyMinutes() != null) {

        doseSchedule.setAllowedEarlyMinutes(
                request.getAllowedEarlyMinutes()
        );
        if (request.getWindowStart() == null) {
            doseSchedule.setWindowStart(resolveTargetTime(doseSchedule).minusMinutes(request.getAllowedEarlyMinutes()));
        }
    }

    if (request.getAllowedLateMinutes() != null) {

        doseSchedule.setAllowedLateMinutes(
                request.getAllowedLateMinutes()
        );
        if (request.getWindowEnd() == null) {
            doseSchedule.setWindowEnd(resolveTargetTime(doseSchedule).plusMinutes(request.getAllowedLateMinutes()));
        }
    }

    if (request.getActive() != null) {

        doseSchedule.setActive(
                request.getActive()
        );
    }

    doseSchedule.setUpdatedAt(
            OffsetDateTime.now()
    );

    doseSchedule.setTargetTime(resolveTargetTime(doseSchedule));
    doseSchedule.setWindowStart(resolveWindowStart(doseSchedule));
    doseSchedule.setWindowEnd(resolveWindowEnd(doseSchedule));
    validateWindow(doseSchedule.getWindowStart(), doseSchedule.getTargetTime(), doseSchedule.getWindowEnd());

    DoseSchedule updatedDoseSchedule =
            doseScheduleRepository.save(
                    doseSchedule
            );

    carePlanService.regenerateAfterPrescriptionChange(doseSchedule.getPatientId());

    return mapToResponse(
            updatedDoseSchedule
    );
}
    public void deactivateDoseSchedule(
        Long scheduleId
    ) {

    DoseSchedule doseSchedule =
            doseScheduleRepository.findById(
                    scheduleId
            ).orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Dose schedule not found with id: "
                                    + scheduleId
                    )
            );

    authorizationService.authorize(
            Permission.WRITE_MANUAL_DOSE,
            doseSchedule.getPatientId()
    );

    doseSchedule.setActive(false);

    doseSchedule.setUpdatedAt(
            OffsetDateTime.now()
    );

    doseScheduleRepository.save(
            doseSchedule
    );

    carePlanService.regenerateAfterPrescriptionChange(doseSchedule.getPatientId());
    }

    public List<ScheduleAdherenceResponse>
    getTodayScheduleAdherence(
        Long patientId
    ) {

    authorizationService.authorize(
            Permission.READ_PATIENT_READINGS,
            patientId
    );

    List<DoseSchedule> schedules =
            doseScheduleRepository
                    .findByPatientIdAndActiveTrue(
                            patientId,
                            Pageable.unpaged()
                    )
                    .getContent();

    ZoneId zoneId =
        ZoneId.systemDefault();

        OffsetDateTime startOfDay =
                LocalDate.now()
                        .atStartOfDay(zoneId)
                        .toOffsetDateTime();

        OffsetDateTime endOfDay =
                startOfDay.plusDays(1);

    List<DoseEvent> todaysDoseEvents =
            doseEventRepository
                    .findByPatientIdAndInjectedAtBetween(
                            patientId,
                            startOfDay,
                            endOfDay
                    );

    List<ScheduleAdherenceResponse> responses =
            new ArrayList<>();

    for (DoseSchedule schedule : schedules) {

        ScheduleAdherenceResponse response =
                new ScheduleAdherenceResponse();

        response.setScheduleId(
                schedule.getScheduleId()
        );

        response.setScheduleLabel(
                schedule.getScheduleLabel()
        );

        response.setScheduledTime(
                schedule.getScheduledTime()
        );

        response.setDoseUnits(
                schedule.getDoseUnits()
        );

        OffsetDateTime scheduledDateTime =
        LocalDate.now()
                .atTime(
                        schedule.getScheduledTime()
                )
                .atZone(zoneId)
                .toOffsetDateTime();

        OffsetDateTime earlyBoundary =
                scheduledDateTime.minusMinutes(
                        schedule.getAllowedEarlyMinutes()
                );

        OffsetDateTime lateBoundary =
                scheduledDateTime.plusMinutes(
                        schedule.getAllowedLateMinutes()
                );

        Optional<DoseEvent> linkedEvent =
                todaysDoseEvents.stream()
                        .filter(event ->
                                schedule.getScheduleId()
                                        .equals(
                                                event.getScheduleId()
                                        )
                        )
                        .min(
                                Comparator.comparing(
                                        DoseEvent::getInjectedAt
                                )
                        );

        if (linkedEvent.isEmpty()) {

            if (OffsetDateTime.now()
                    .isBefore(lateBoundary)) {

                response.setStatus(
                        "PENDING"
                );

            } else {

                response.setStatus(
                        "MISSED"
                );
            }

            responses.add(response);

            continue;
        }

        DoseEvent event =
                linkedEvent.get();

        response.setInjectedAt(
                event.getInjectedAt()
        );

        long minuteOffset =
                Duration.between(
                        scheduledDateTime,
                        event.getInjectedAt()
                ).toMinutes();

        response.setMinutesOffset(
                minuteOffset
        );

        /*
         * FUTURE EXTENSIONS
         *
         * Possible improvements:
         *
         * 1. Detect emergency correction doses
         * 2. Detect overlapping injections
         * 3. Weekly adherence scoring
         * 4. AI-based adherence pattern analysis
         * 5. Hypoglycemia-aware schedule tolerance
         * 6. Multi-dose clustering
         * 7. Smart auto-linking suggestions
         */

        if (event.getInjectedAt()
                .isBefore(earlyBoundary)) {

            response.setStatus(
                    "EARLY"
            );

        } else if (event.getInjectedAt()
                .isAfter(lateBoundary)) {

            response.setStatus(
                    "LATE"
            );

        } else {

            response.setStatus(
                    "ON_TIME"
            );
        }

        responses.add(response);
    }

    return responses;
    }

    private LocalTime resolveWindowStart(DoseSchedule schedule) {
        if (schedule.getWindowStart() != null) {
            return schedule.getWindowStart();
        }

        return resolveTargetTime(schedule).minusMinutes(defaultInteger(schedule.getAllowedEarlyMinutes(), 60));
    }

    private LocalTime resolveTargetTime(DoseSchedule schedule) {
        if (schedule.getTargetTime() != null) {
            return schedule.getTargetTime();
        }

        return schedule.getScheduledTime();
    }

    private LocalTime resolveWindowEnd(DoseSchedule schedule) {
        if (schedule.getWindowEnd() != null) {
            return schedule.getWindowEnd();
        }

        return resolveTargetTime(schedule).plusMinutes(defaultInteger(schedule.getAllowedLateMinutes(), 120));
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Dose schedule target time is required");
        }

        return LocalTime.parse(value);
    }

    private LocalTime parseTimeOrDefault(String value, LocalTime defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return LocalTime.parse(value);
    }

    private Integer defaultInteger(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        return second;
    }

    private void validateWindow(LocalTime windowStart, LocalTime targetTime, LocalTime windowEnd) {
        if (windowStart == null || targetTime == null || windowEnd == null) {
            throw new IllegalArgumentException("Dose schedule window start, target time and window end are required");
        }

        if (windowStart.isAfter(targetTime) || targetTime.isAfter(windowEnd)) {
            throw new IllegalArgumentException("Dose schedule window must satisfy windowStart <= targetTime <= windowEnd");
        }
    }
}
