package com.diasmart.springapi.users.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser appUser;

    @Column(name = "inventory_alerts", nullable = false)
    private boolean inventoryAlerts = true;

    @Column(name = "temperature_alerts", nullable = false)
    private boolean temperatureAlerts = true;

    @Column(name = "missed_dose_alerts", nullable = false)
    private boolean missedDoseAlerts = true;

    @Column(name = "email_notifications", nullable = false)
    private boolean emailNotifications = true;

    @Column(name = "sms_notifications", nullable = false)
    private boolean smsNotifications = false;

    @Column(name = "two_factor_auth", nullable = false)
    private boolean twoFactorAuth = false;

    public UserSettings() {
    }

    public UserSettings(AppUser appUser) {
        this.appUser = appUser;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getAppUser() {
        return appUser;
    }

    public void setAppUser(AppUser appUser) {
        this.appUser = appUser;
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
