package com.diasmart.springapi.dashboard.service.bridge;

import com.diasmart.springapi.glucose.dto.GlucoseReadingResponse;
import com.diasmart.springapi.glucose.entity.GlucoseReading;
import com.diasmart.springapi.glucose.repository.GlucoseReadingRepository;

import org.springframework.stereotype.Service;

@Service
public class GlucoseServiceBridge {

    private final GlucoseReadingRepository glucoseReadingRepository;

    public GlucoseServiceBridge(
            GlucoseReadingRepository glucoseReadingRepository
    ) {
        this.glucoseReadingRepository =
                glucoseReadingRepository;
    }

    public GlucoseReadingResponse
    getLatestReading(
            Long patientId
    ) {

        GlucoseReading reading =
                glucoseReadingRepository
                        .findTopByPatientIdOrderByMeasuredAtDesc(
                                patientId
                        );

        if (reading == null) {
            return null;
        }

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