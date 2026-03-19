package com.gym.tracking.repository;

import com.gym.tracking.entity.DietLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DietLogRepository extends JpaRepository<DietLog, Long> {
    List<DietLog> findByUserId(Long userId);
    List<DietLog> findByUserIdAndLogDate(Long userId, LocalDate logDate);
    List<DietLog> findByUserIdAndLogDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
}
