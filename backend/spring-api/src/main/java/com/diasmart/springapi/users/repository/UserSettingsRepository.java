package com.diasmart.springapi.users.repository;

import com.diasmart.springapi.users.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
    Optional<UserSettings> findByAppUser_UserId(Long userId);
}
