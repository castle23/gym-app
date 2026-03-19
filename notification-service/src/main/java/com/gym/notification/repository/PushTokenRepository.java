package com.gym.notification.repository;

import com.gym.notification.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushTokenRepository extends JpaRepository<PushToken, Long> {
    Optional<PushToken> findByToken(String token);
    
    List<PushToken> findByUserId(Long userId);
    
    List<PushToken> findByUserIdAndIsActiveTrue(Long userId);
    
    List<PushToken> findByUserIdAndDeviceTypeAndIsActiveTrue(Long userId, String deviceType);
    
    void deleteByUserIdAndToken(Long userId, String token);
}
