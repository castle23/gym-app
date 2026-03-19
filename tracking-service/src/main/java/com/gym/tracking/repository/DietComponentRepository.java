package com.gym.tracking.repository;

import com.gym.tracking.entity.DietComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DietComponentRepository extends JpaRepository<DietComponent, Long> {
    Optional<DietComponent> findByPlanId(Long planId);
}
