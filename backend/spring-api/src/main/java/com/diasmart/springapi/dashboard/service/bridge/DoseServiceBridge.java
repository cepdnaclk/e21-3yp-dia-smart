package com.diasmart.springapi.dashboard.service.bridge;

import com.diasmart.springapi.dose.dto.DoseEventResponse;
import com.diasmart.springapi.dose.entity.DoseEvent;
import com.diasmart.springapi.dose.repository.DoseEventRepository;

import org.springframework.stereotype.Service;

@Service
public class DoseServiceBridge {

    private final DoseEventRepository doseEventRepository;

    public DoseServiceBridge(
            DoseEventRepository doseEventRepository
    ) {
        this.doseEventRepository =
                doseEventRepository;
    }

    public DoseEventResponse
    getLatestDose(
            Long patientId
    ) {

        DoseEvent event =
                doseEventRepository
                        .findTopByPatientIdOrderByInjectedAtDesc(
                                patientId
                        );

        if (event == null) {
            return null;
        }

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
