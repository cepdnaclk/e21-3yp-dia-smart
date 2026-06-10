package com.diasmart.springapi.dose.service;

import com.diasmart.springapi.dose.dto.CreateManualDoseEventRequest;
import com.diasmart.springapi.dose.dto.DoseEventResponse;
import com.diasmart.springapi.dose.entity.DoseEvent;
import com.diasmart.springapi.dose.repository.DoseEventRepository;
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class DoseEventService {

    private final DoseEventRepository doseEventRepository;

    private final AuthorizationService authorizationService;

    public DoseEventService(
            DoseEventRepository doseEventRepository,
            AuthorizationService authorizationService
    ) {
        this.doseEventRepository = doseEventRepository;
        this.authorizationService = authorizationService;
    }

    public Page<DoseEventResponse> getDoseEvents(
            Long patientId,
            Pageable pageable
    ) {

        authorizationService.authorize(
                Permission.READ_PATIENT_READINGS,
                patientId
        );

        return doseEventRepository
                .findByPatientIdOrderByInjectedAtDesc(
                        patientId,
                        pageable
                )
                .map(this::mapToResponse);
    }

    public DoseEventResponse createManualDoseEvent(
            Long patientId,
            CreateManualDoseEventRequest request
    ) {
        authorizationService.authorize(
        Permission.WRITE_MANUAL_DOSE,
        patientId
        );

        DoseEvent event = new DoseEvent();

        event.setPatientId(patientId);

        event.setDoseUnits(
                request.getDoseUnits()
        );

        event.setInjectedAt(
                OffsetDateTime.parse(
                        request.getInjectedAt()
                )
        );

        event.setNotes(
                request.getNotes()
        );

        // manual entry distinction
        event.setDetectionMethod("MANUAL");

        event.setEventStatus("CONFIRMED");

        event.setCreatedAt(
                OffsetDateTime.now()
        );

        DoseEvent savedEvent =
                doseEventRepository.save(event);

        return mapToResponse(savedEvent);
    }

    private DoseEventResponse mapToResponse(
            DoseEvent event
    ) {

        DoseEventResponse response =
                new DoseEventResponse();

        response.setDoseEventId(
                event.getDoseEventId()
        );

        response.setDoseUnits(
                event.getDoseUnits()
        );

        response.setInjectedAt(
                event.getInjectedAt()
        );

        response.setDetectionMethod(
                event.getDetectionMethod()
        );

        response.setEventStatus(
                event.getEventStatus()
        );

        response.setNotes(
                event.getNotes()
        );

        response.setCreatedAt(
                event.getCreatedAt()
        );

        return response;
    }
}