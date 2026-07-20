package com.diasmart.springapi.users.service;

import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.dto.UserSettingsDto;
import com.diasmart.springapi.users.entity.AppUser;
import com.diasmart.springapi.users.entity.UserSettings;
import com.diasmart.springapi.users.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSettingsService {

    private final CurrentUserService currentUserService;
    private final UserSettingsRepository userSettingsRepository;

    public UserSettingsService(
            CurrentUserService currentUserService,
            UserSettingsRepository userSettingsRepository) {
        this.currentUserService = currentUserService;
        this.userSettingsRepository = userSettingsRepository;
    }

    @Transactional
    public UserSettings getUserSettings() {
        AppUser user = currentUserService.getCurrentUser();
        return userSettingsRepository.findByAppUser(user)
                .orElseGet(() -> {
                    UserSettings settings = new UserSettings(user);
                    return userSettingsRepository.save(settings);
                });
    }

    @Transactional
    public UserSettings updateUserSettings(UserSettingsDto dto) {
        UserSettings settings = getUserSettings();
        settings.setInventoryAlerts(dto.isInventoryAlerts());
        settings.setTemperatureAlerts(dto.isTemperatureAlerts());
        settings.setMissedDoseAlerts(dto.isMissedDoseAlerts());
        settings.setEmailNotifications(dto.isEmailNotifications());
        settings.setSmsNotifications(dto.isSmsNotifications());
        settings.setTwoFactorAuth(dto.isTwoFactorAuth());
        return userSettingsRepository.save(settings);
    }
}
