package com.diasmart.springapi.patients.repository;

import com.diasmart.springapi.patients.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
}