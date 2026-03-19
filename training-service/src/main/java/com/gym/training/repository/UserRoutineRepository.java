package com.gym.training.repository;

import com.gym.training.entity.UserRoutine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoutineRepository extends JpaRepository<UserRoutine, Long> {
    Page<UserRoutine> findByUserId(Long userId, Pageable pageable);
    Page<UserRoutine> findByUserIdAndIsActive(Long userId, Boolean isActive, Pageable pageable);
    List<UserRoutine> findByUserIdAndIsActive(Long userId, Boolean isActive);
    Optional<UserRoutine> findByIdAndUserId(Long id, Long userId);
}

