package com.gym.training.repository;

import com.gym.training.entity.ExerciseSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ExerciseSessionRepository extends JpaRepository<ExerciseSession, Long> {
    Page<ExerciseSession> findByUserRoutineId(Long userRoutineId, Pageable pageable);
    
    @Query("SELECT es FROM ExerciseSession es WHERE es.userRoutine.userId = :userId " +
           "AND DATE(es.sessionDate) = :date")
    Page<ExerciseSession> findByUserIdAndDate(
        @Param("userId") Long userId,
        @Param("date") LocalDate date,
        Pageable pageable
    );
    
    Optional<ExerciseSession> findByIdAndUserRoutine_UserId(Long id, Long userId);
}

