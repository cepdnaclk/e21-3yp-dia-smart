package com.diasmart.springapi.dose_schedules.service;

import com.diasmart.springapi.dose_schedules.dto.DoseScheduleResponse;
import com.diasmart.springapi.dose_schedules.entity.DoseSchedule;
import com.diasmart.springapi.dose_schedules.repository.DoseScheduleRepository;
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

    public DoseScheduleService(
            DoseScheduleRepository doseScheduleRepository,
            AuthorizationService authorizationService,
            DoseEventRepository doseEventRepository
    ) {
        this.doseScheduleRepository = doseScheduleRepository;
        this.authorizationService = authorizationService;
        this.doseEventRepository = doseEventRepository;
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

    doseSchedule.setScheduledTime(
            LocalTime.parse(
                    request.getScheduledTime()
            )
    );

    doseSchedule.setDoseUnits(
            request.getDoseUnits()
    );

    doseSchedule.setDaysOfWeek(
            request.getDaysOfWeek()
    );

    doseSchedule.setAllowedEarlyMinutes(
            request.getAllowedEarlyMinutes()
    );

    doseSchedule.setAllowedLateMinutes(
            request.getAllowedLateMinutes()
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

        doseSchedule.setScheduledTime(
                LocalTime.parse(
                        request.getScheduledTime()
                )
        );
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
    }

    if (request.getAllowedLateMinutes() != null) {

        doseSchedule.setAllowedLateMinutes(
                request.getAllowedLateMinutes()
        );
    }

    if (request.getActive() != null) {

        doseSchedule.setActive(
                request.getActive()
        );
    }

    doseSchedule.setUpdatedAt(
            OffsetDateTime.now()
    );

    DoseSchedule updatedDoseSchedule =
            doseScheduleRepository.save(
                    doseSchedule
            );

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
}