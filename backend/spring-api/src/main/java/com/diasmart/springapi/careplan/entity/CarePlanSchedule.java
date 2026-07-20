package com.diasmart.springapi.careplan.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "care_plan_schedules")
public class CarePlanSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "care_plan_schedule_id")
    private Long carePlanScheduleId;

    @Column(name = "snapshot_id", nullable = false)
    private Long snapshotId;

    @Column(name = "source_schedule_id", nullable = false)
    private Long sourceScheduleId;

    @Column(name = "schedule_external_id", nullable = false, length = 80)
    private String scheduleExternalId;

    @Column(name = "period", nullable = false, length = 30)
    private String period;

    @Column(name = "insulin_type", nullable = false, length = 120)
    private String insulinType;

    @Column(name = "dose_units", nullable = false)
    private BigDecimal doseUnits;

    @Column(name = "window_start", nullable = false)
    private LocalTime windowStart;

    @Column(name = "target_time", nullable = false)
    private LocalTime targetTime;

    @Column(name = "window_end", nullable = false)
    private LocalTime windowEnd;

    public Long getCarePlanScheduleId() {
        return carePlanScheduleId;
    }

    public void setCarePlanScheduleId(Long carePlanScheduleId) {
        this.carePlanScheduleId = carePlanScheduleId;
    }

    public Long getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(Long snapshotId) {
        this.snapshotId = snapshotId;
    }

    public Long getSourceScheduleId() {
        return sourceScheduleId;
    }

    public void setSourceScheduleId(Long sourceScheduleId) {
        this.sourceScheduleId = sourceScheduleId;
    }

    public String getScheduleExternalId() {
        return scheduleExternalId;
    }

    public void setScheduleExternalId(String scheduleExternalId) {
        this.scheduleExternalId = scheduleExternalId;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getInsulinType() {
        return insulinType;
    }

    public void setInsulinType(String insulinType) {
        this.insulinType = insulinType;
    }

    public BigDecimal getDoseUnits() {
        return doseUnits;
    }

    public void setDoseUnits(BigDecimal doseUnits) {
        this.doseUnits = doseUnits;
    }

    public LocalTime getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(LocalTime windowStart) {
        this.windowStart = windowStart;
    }

    public LocalTime getTargetTime() {
        return targetTime;
    }

    public void setTargetTime(LocalTime targetTime) {
        this.targetTime = targetTime;
    }

    public LocalTime getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(LocalTime windowEnd) {
        this.windowEnd = windowEnd;
    }
}
