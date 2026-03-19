package com.gym.tracking.repository;

import com.gym.tracking.entity.MeasurementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeasurementTypeRepository extends JpaRepository<MeasurementType, Long> {
    List<MeasurementType> findByType(String type);
    Optional<MeasurementType> findByTypeAndSystemType(String type, Boolean isSystem);
}
