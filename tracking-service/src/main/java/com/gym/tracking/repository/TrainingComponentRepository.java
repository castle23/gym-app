package com.gym.tracking.repository;

import com.gym.tracking.entity.TrainingComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrainingComponentRepository extends JpaRepository<TrainingComponent, Long> {
    Optional<TrainingComponent> findByPlanId(Long planId);
}
