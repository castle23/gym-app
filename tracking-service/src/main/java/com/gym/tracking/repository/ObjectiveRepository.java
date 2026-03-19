package com.gym.tracking.repository;

import com.gym.tracking.entity.Objective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ObjectiveRepository extends JpaRepository<Objective, Long> {
    List<Objective> findByUserId(Long userId);
    Optional<Objective> findByIdAndUserId(Long id, Long userId);
    List<Objective> findByUserIdAndIsActive(Long userId, Boolean isActive);
}
