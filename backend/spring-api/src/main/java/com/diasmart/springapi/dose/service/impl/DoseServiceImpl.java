package com.diasmart.springapi.dose.service.impl;

import com.diasmart.springapi.dose.dto.DoseEventResponseDto;
import com.diasmart.springapi.dose.entity.DoseEvent;
import com.diasmart.springapi.dose.repository.DoseEventRepository;
import com.diasmart.springapi.dose.service.DoseService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;


// Registers this class as Spring service bean
@Service

// Auto-generates constructor for final fields
@RequiredArgsConstructor
public class DoseServiceImpl implements DoseService {

    // Repository dependency
    private final DoseEventRepository doseEventRepository;


    // Fetch patient dose history
    @Override
    public Page<DoseEventResponseDto> getPatientDoseEvents(
            Long patientId,
            Pageable pageable
    ) {

        return doseEventRepository
                .findByPatientIdOrderByInjectedAtDesc(
                        patientId,
                        pageable
                )
                .map(this::mapToDto);
    }


    // Convert entity into response DTO
    private DoseEventResponseDto mapToDto(
            DoseEvent doseEvent
    ) {

        return DoseEventResponseDto.builder()

                .doseEventId(doseEvent.getDoseEventId())

                .patientId(doseEvent.getPatientId())

                .doseUnits(doseEvent.getDoseUnits())

                .injectedAt(doseEvent.getInjectedAt())

                .detectionMethod(
                        doseEvent.getDetectionMethod()
                )

                .eventStatus(
                        doseEvent.getEventStatus()
                )

                .build();
    }
}