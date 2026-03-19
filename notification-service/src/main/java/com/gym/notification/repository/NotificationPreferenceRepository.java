package com.gym.notification.repository;

import com.gym.notification.entity.NotificationPreference;
import com.gym.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    Optional<NotificationPreference> findByUserIdAndNotificationType(Long userId, NotificationType type);
    
    List<NotificationPreference> findByUserId(Long userId);
}
