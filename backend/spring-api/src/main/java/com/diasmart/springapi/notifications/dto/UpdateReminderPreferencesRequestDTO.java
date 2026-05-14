package com.diasmart.springapi.notifications.dto;

import jakarta.validation.constraints.Min;

public class UpdateReminderPreferencesRequestDTO {

    private Boolean remindersEnabled;

    @Min(0)
    private Integer reminderLeadMinutes;

    @Min(0)
    private Integer reminderMinutesBefore;

    public Boolean getRemindersEnabled() {
        return remindersEnabled;
    }

    public void setRemindersEnabled(Boolean remindersEnabled) {
        this.remindersEnabled = remindersEnabled;
    }

    public Integer getReminderLeadMinutes() {
        return reminderLeadMinutes;
    }

    public void setReminderLeadMinutes(
            Integer reminderLeadMinutes
    ) {
        this.reminderLeadMinutes = reminderLeadMinutes;
    }

    public Integer getReminderMinutesBefore() {
        return reminderMinutesBefore;
    }

    public void setReminderMinutesBefore(
            Integer reminderMinutesBefore
    ) {
        this.reminderMinutesBefore = reminderMinutesBefore;
    }
}
