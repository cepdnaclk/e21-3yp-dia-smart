package com.diasmart.springapi.careplan.repository;

import com.diasmart.springapi.careplan.entity.CarePlanSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarePlanScheduleRepository extends JpaRepository<CarePlanSchedule, Long> {

    List<CarePlanSchedule> findBySnapshotId(Long snapshotId);

    Optional<CarePlanSchedule> findBySnapshotIdAndScheduleExternalId(Long snapshotId, String scheduleExternalId);
}
