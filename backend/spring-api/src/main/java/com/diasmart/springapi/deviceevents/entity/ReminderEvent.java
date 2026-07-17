package com.diasmart.springapi.deviceevents.entity;

import jakarta.persistence.*;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "reminder_events")
public class ReminderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reminder_event_id")
    private Long reminderEventId;

    @Column(name = "telemetry_event_id")
    private Long telemetryEventId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "outer_device_id")
    private Long outerDeviceId;

    @Column(name = "source_schedule_id")
    private Long sourceScheduleId;

    @Column(name = "schedule_external_id", length = 80)
    private String scheduleExternalId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "care_plan_version")
    private Integer carePlanVersion;

    @Column(name = "repeat_number")
    private Integer repeatNumber;

    @Column(name = "window_start")
    private LocalTime windowStart;

    @Column(name = "target_time")
    private LocalTime targetTime;

    @Column(name = "window_end")
    private LocalTime windowEnd;

    @Column(name = "event_timestamp", nullable = false)
    private OffsetDateTime eventTimestamp;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public Long getReminderEventId() {
        return reminderEventId;
    }

    public void setReminderEventId(Long reminderEventId) {
        this.reminderEventId = reminderEventId;
    }

    public Long getTelemetryEventId() {
        return telemetryEventId;
    }

    public void setTelemetryEventId(Long telemetryEventId) {
        this.telemetryEventId = telemetryEventId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getOuterDeviceId() {
        return outerDeviceId;
    }

    public void setOuterDeviceId(Long outerDeviceId) {
        this.outerDeviceId = outerDeviceId;
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Integer getCarePlanVersion() {
        return carePlanVersion;
    }

    public void setCarePlanVersion(Integer carePlanVersion) {
        this.carePlanVersion = carePlanVersion;
    }

    public Integer getRepeatNumber() {
        return repeatNumber;
    }

    public void setRepeatNumber(Integer repeatNumber) {
        this.repeatNumber = repeatNumber;
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

    public OffsetDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(OffsetDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
