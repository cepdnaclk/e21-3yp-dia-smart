package com.diasmart.springapi.users.repository;

import com.diasmart.springapi.users.entity.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, Long> {

    List<UserDeviceToken> findByUserId(Long userId);

    Optional<UserDeviceToken> findByDeviceToken(String deviceToken);

    void deleteByDeviceToken(String deviceToken);

    void deleteByUserIdAndDeviceToken(Long userId, String deviceToken);
}
