package com.diasmart.springapi.users.service;

import com.diasmart.springapi.users.dto.RegisterDeviceTokenRequest;
import com.diasmart.springapi.users.entity.UserDeviceToken;
import com.diasmart.springapi.users.repository.UserDeviceTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DeviceTokenService {

    private final UserDeviceTokenRepository userDeviceTokenRepository;

    public DeviceTokenService(UserDeviceTokenRepository userDeviceTokenRepository) {
        this.userDeviceTokenRepository = userDeviceTokenRepository;
    }

    @Transactional
    public UserDeviceToken registerToken(Long userId, RegisterDeviceTokenRequest request) {
        String token = request.getDeviceToken().trim();
        String platform = request.getPlatform() != null ? request.getPlatform().trim().toLowerCase() : "android";

        Optional<UserDeviceToken> existingToken = userDeviceTokenRepository.findByDeviceToken(token);

        UserDeviceToken deviceToken;
        if (existingToken.isPresent()) {
            deviceToken = existingToken.get();
            deviceToken.setUserId(userId);
            deviceToken.setPlatform(platform);
        } else {
            deviceToken = new UserDeviceToken(userId, token, platform);
        }

        return userDeviceTokenRepository.save(deviceToken);
    }

    @Transactional
    public void unregisterToken(Long userId, String token) {
        if (token != null && !token.isBlank()) {
            userDeviceTokenRepository.deleteByUserIdAndDeviceToken(userId, token.trim());
        }
    }

    @Transactional(readOnly = true)
    public List<UserDeviceToken> getUserTokens(Long userId) {
        return userDeviceTokenRepository.findByUserId(userId);
    }
}
