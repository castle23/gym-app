package com.gym.tracking.repository;

import com.gym.tracking.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByUserId(Long userId);
    Optional<Plan> findByIdAndUserId(Long id, Long userId);
    Optional<Plan> findByUserIdAndStatus(Long userId, Plan.PlanStatus status);
    List<Plan> findByUserIdAndStatus(Long userId, Plan.PlanStatus status);
}
