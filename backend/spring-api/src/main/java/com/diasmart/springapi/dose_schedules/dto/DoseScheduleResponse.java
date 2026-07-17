package com.diasmart.springapi.dose_schedules.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public class DoseScheduleResponse {

    private Long scheduleId;

    private Long prescriptionId;

    private String scheduleLabel;

    private LocalTime scheduledTime;

    private LocalTime windowStart;

    private LocalTime targetTime;

    private LocalTime windowEnd;

    private BigDecimal doseUnits;

    private String daysOfWeek;

    private Integer allowedEarlyMinutes;

    private Integer allowedLateMinutes;

    private Boolean active;

    private OffsetDateTime createdAt;

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public String getScheduleLabel() {
        return scheduleLabel;
    }

    public void setScheduleLabel(String scheduleLabel) {
        this.scheduleLabel = scheduleLabel;
    }

    public LocalTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalTime scheduledTime) {
        this.scheduledTime = scheduledTime;
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

    public BigDecimal getDoseUnits() {
        return doseUnits;
    }

    public void setDoseUnits(BigDecimal doseUnits) {
        this.doseUnits = doseUnits;
    }

    public String getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(String daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public Integer getAllowedEarlyMinutes() {
        return allowedEarlyMinutes;
    }

    public void setAllowedEarlyMinutes(Integer allowedEarlyMinutes) {
        this.allowedEarlyMinutes = allowedEarlyMinutes;
    }

    public Integer getAllowedLateMinutes() {
        return allowedLateMinutes;
    }

    public void setAllowedLateMinutes(Integer allowedLateMinutes) {
        this.allowedLateMinutes = allowedLateMinutes;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
