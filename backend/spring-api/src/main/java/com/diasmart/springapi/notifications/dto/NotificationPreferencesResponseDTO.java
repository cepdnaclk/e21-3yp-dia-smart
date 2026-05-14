package com.diasmart.springapi.notifications.dto;

import java.time.OffsetDateTime;

public class NotificationPreferencesResponseDTO {

    private Long preferenceId;
    private Long userId;
    private Boolean remindersEnabled;
    private Boolean buzzerEnabled;
    private Integer reminderLeadMinutes;
    private Boolean mobileNotificationsEnabled;
    private Boolean dashboardNotificationsEnabled;
    private Boolean smsEnabled;
    private Boolean emailEnabled;
    private Boolean caregiverNotificationsEnabled;
    private OffsetDateTime updatedAt;

    public Long getPreferenceId() {
        return preferenceId;
    }

    public void setPreferenceId(Long preferenceId) {
        this.preferenceId = preferenceId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getRemindersEnabled() {
        return remindersEnabled;
    }

    public void setRemindersEnabled(Boolean remindersEnabled) {
        this.remindersEnabled = remindersEnabled;
    }

    public Boolean getBuzzerEnabled() {
        return buzzerEnabled;
    }

    public void setBuzzerEnabled(Boolean buzzerEnabled) {
        this.buzzerEnabled = buzzerEnabled;
    }

    public Integer getReminderLeadMinutes() {
        return reminderLeadMinutes;
    }

    public void setReminderLeadMinutes(
            Integer reminderLeadMinutes
    ) {
        this.reminderLeadMinutes = reminderLeadMinutes;
    }

    public Boolean getMobileNotificationsEnabled() {
        return mobileNotificationsEnabled;
    }

    public void setMobileNotificationsEnabled(
            Boolean mobileNotificationsEnabled
    ) {
        this.mobileNotificationsEnabled =
                mobileNotificationsEnabled;
    }

    public Boolean getDashboardNotificationsEnabled() {
        return dashboardNotificationsEnabled;
    }

    public void setDashboardNotificationsEnabled(
            Boolean dashboardNotificationsEnabled
    ) {
        this.dashboardNotificationsEnabled =
                dashboardNotificationsEnabled;
    }

    public Boolean getSmsEnabled() {
        return smsEnabled;
    }

    public void setSmsEnabled(Boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
    }

    public Boolean getEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(Boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public Boolean getCaregiverNotificationsEnabled() {
        return caregiverNotificationsEnabled;
    }

    public void setCaregiverNotificationsEnabled(
            Boolean caregiverNotificationsEnabled
    ) {
        this.caregiverNotificationsEnabled =
                caregiverNotificationsEnabled;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
