package com.diasmart.springapi.notifications.dto;

public class UpdateNotificationPreferencesRequestDTO {

    private Boolean remindersEnabled;
    private Boolean reminderEnabled;
    private Boolean buzzerEnabled;
    private Boolean mobileNotificationsEnabled;
    private Boolean pushEnabled;
    private Boolean dashboardNotificationsEnabled;
    private Boolean smsEnabled;
    private Boolean emailEnabled;
    private Boolean caregiverNotificationsEnabled;

    public Boolean getRemindersEnabled() {
        return remindersEnabled;
    }

    public void setRemindersEnabled(Boolean remindersEnabled) {
        this.remindersEnabled = remindersEnabled;
    }

    public Boolean getReminderEnabled() {
        return reminderEnabled;
    }

    public void setReminderEnabled(Boolean reminderEnabled) {
        this.reminderEnabled = reminderEnabled;
    }

    public Boolean getBuzzerEnabled() {
        return buzzerEnabled;
    }

    public void setBuzzerEnabled(Boolean buzzerEnabled) {
        this.buzzerEnabled = buzzerEnabled;
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

    public Boolean getPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(Boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
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
}
