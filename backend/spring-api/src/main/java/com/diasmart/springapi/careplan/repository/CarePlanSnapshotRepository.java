package com.diasmart.springapi.careplan.repository;

import com.diasmart.springapi.careplan.entity.CarePlanSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarePlanSnapshotRepository extends JpaRepository<CarePlanSnapshot, Long> {

    Optional<CarePlanSnapshot> findTopByPatientIdOrderByVersionDesc(Long patientId);

    Optional<CarePlanSnapshot> findByPatientIdAndVersion(Long patientId, Integer version);

    Optional<CarePlanSnapshot> findByCarePlanUid(String carePlanUid);
}
