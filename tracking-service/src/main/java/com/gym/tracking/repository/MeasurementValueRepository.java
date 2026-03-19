package com.gym.tracking.repository;

import com.gym.tracking.entity.MeasurementValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MeasurementValueRepository extends JpaRepository<MeasurementValue, Long> {
    List<MeasurementValue> findByUserIdAndMeasurementTypeId(Long userId, Long measurementTypeId);
    List<MeasurementValue> findByUserIdAndMeasurementTypeIdAndMeasurementDateBetween(
            Long userId, Long measurementTypeId, LocalDate startDate, LocalDate endDate);
    List<MeasurementValue> findByUserId(Long userId);
}
