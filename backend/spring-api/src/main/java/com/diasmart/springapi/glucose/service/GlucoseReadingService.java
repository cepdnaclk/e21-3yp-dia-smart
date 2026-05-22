package com.diasmart.springapi.glucose.service;

import com.diasmart.springapi.glucose.dto.CreateManualGlucoseReadingRequest;
import com.diasmart.springapi.glucose.dto.GlucoseReadingResponse;
import com.diasmart.springapi.glucose.entity.GlucoseReading;
import com.diasmart.springapi.glucose.repository.GlucoseReadingRepository;
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class GlucoseReadingService {

    private final GlucoseReadingRepository glucoseReadingRepository;

    private final AuthorizationService authorizationService;

    public GlucoseReadingService(
            GlucoseReadingRepository glucoseReadingRepository,
            AuthorizationService authorizationService
    ) {
        this.glucoseReadingRepository = glucoseReadingRepository;
        this.authorizationService = authorizationService;
    }

    public Page<GlucoseReadingResponse> getPatientReadings(
            Long patientId,
            Pageable pageable
    ) {

        authorizationService.authorize(
                Permission.READ_PATIENT_READINGS,
                patientId
        );

        return glucoseReadingRepository
                .findByPatientId(patientId, pageable)
                .map(this::mapToResponse);
    }

    public GlucoseReadingResponse createManualReading(
            Long patientId,
            CreateManualGlucoseReadingRequest request
    ) {

        // authorizationService.authorize(
        //         Permission.WRITE_MANUAL_GLUCOSE,
        //         patientId
        // );

        GlucoseReading reading = new GlucoseReading();

        reading.setPatientId(patientId);

        reading.setGlucoseValueMgDl(
                request.getGlucoseValueMgDl()
        );

        reading.setMeasuredAt(
                request.getMeasuredAt()
        );

        reading.setMealContext(
                request.getMealContext()
        );

        reading.setNotes(
                request.getNotes()
        );

        // Important:
        // distinguish manual entries from device telemetry
        reading.setSource("MANUAL");

        reading.setCreatedAt(
                OffsetDateTime.now()
        );


        GlucoseReading savedReading =
        glucoseReadingRepository.save(reading);


        return mapToResponse(savedReading);
        }

    private GlucoseReadingResponse mapToResponse(
            GlucoseReading reading
    ) {

        GlucoseReadingResponse response =
                new GlucoseReadingResponse();

        response.setGlucoseReadingId(
                reading.getGlucoseReadingId()
        );

        response.setGlucoseValueMgDl(
                reading.getGlucoseValueMgDl()
        );

        response.setMeasuredAt(
                reading.getMeasuredAt()
        );

        response.setSource(
                reading.getSource()
        );

        response.setMealContext(
                reading.getMealContext()
        );

        response.setNotes(
                reading.getNotes()
        );

        response.setCreatedAt(
                reading.getCreatedAt()
        );

        return response;
    }
}