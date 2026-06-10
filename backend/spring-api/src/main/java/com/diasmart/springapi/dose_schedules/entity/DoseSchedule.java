package com.diasmart.springapi.dose_schedules.entity;
import com.diasmart.springapi.dose_schedules.dto.CreateDoseScheduleRequest;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "dose_schedules")
public class DoseSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "prescription_id")
    private Long prescriptionId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "schedule_label")
    private String scheduleLabel;

    @Column(name = "scheduled_time")
    private LocalTime scheduledTime;

    @Column(name = "dose_units")
    private BigDecimal doseUnits;

    @Column(name = "days_of_week")
    private String daysOfWeek;

    @Column(name = "allowed_early_minutes")
    private Integer allowedEarlyMinutes;

    @Column(name = "allowed_late_minutes")
    private Integer allowedLateMinutes;

    @Column(name = "is_active")
    private Boolean active;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

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

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
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

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}