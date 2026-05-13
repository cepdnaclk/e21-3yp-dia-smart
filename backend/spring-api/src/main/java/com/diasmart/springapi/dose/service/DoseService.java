package com.diasmart.springapi.dose.service;

import com.diasmart.springapi.dose.dto.DoseEventResponseDto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


// Service interface for dose business logic
public interface DoseService {

    // Fetch paginated dose history of patient
    Page<DoseEventResponseDto> getPatientDoseEvents(
            Long patientId,
            Pageable pageable
    );
}