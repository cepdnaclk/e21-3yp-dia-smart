package com.diasmart.springapi.users.dto;

import com.diasmart.springapi.users.entity.UserSettings;

public class UserSettingsDto {
    private boolean inventoryAlerts;
    private boolean temperatureAlerts;
    private boolean missedDoseAlerts;
    private boolean emailNotifications;
    private boolean smsNotifications;
    private boolean twoFactorAuth;

    public UserSettingsDto() {
    }

    public static UserSettingsDto fromEntity(UserSettings entity) {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setInventoryAlerts(entity.isInventoryAlerts());
        dto.setTemperatureAlerts(entity.isTemperatureAlerts());
        dto.setMissedDoseAlerts(entity.isMissedDoseAlerts());
        dto.setEmailNotifications(entity.isEmailNotifications());
        dto.setSmsNotifications(entity.isSmsNotifications());
        dto.setTwoFactorAuth(entity.isTwoFactorAuth());
        return dto;
    }

    public boolean isInventoryAlerts() {
        return inventoryAlerts;
    }

    public void setInventoryAlerts(boolean inventoryAlerts) {
        this.inventoryAlerts = inventoryAlerts;
    }

    public boolean isTemperatureAlerts() {
        return temperatureAlerts;
    }

    public void setTemperatureAlerts(boolean temperatureAlerts) {
        this.temperatureAlerts = temperatureAlerts;
    }

    public boolean isMissedDoseAlerts() {
        return missedDoseAlerts;
    }

    public void setMissedDoseAlerts(boolean missedDoseAlerts) {
        this.missedDoseAlerts = missedDoseAlerts;
    }

    public boolean isEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public boolean isSmsNotifications() {
        return smsNotifications;
    }

    public void setSmsNotifications(boolean smsNotifications) {
        this.smsNotifications = smsNotifications;
    }

    public boolean isTwoFactorAuth() {
        return twoFactorAuth;
    }

    public void setTwoFactorAuth(boolean twoFactorAuth) {
        this.twoFactorAuth = twoFactorAuth;
    }
}
