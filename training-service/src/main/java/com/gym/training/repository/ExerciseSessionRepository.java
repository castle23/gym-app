package com.gym.training.repository;

import com.gym.training.entity.ExerciseSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseSessionRepository extends JpaRepository<ExerciseSession, Long> {
    List<ExerciseSession> findByUserRoutineId(Long userRoutineId);
    
    @Query("SELECT es FROM ExerciseSession es WHERE es.userRoutine.userId = :userId " +
           "AND DATE(es.sessionDate) = :date")
    List<ExerciseSession> findByUserIdAndDate(
        @Param("userId") Long userId,
        @Param("date") LocalDate date
    );
    
    Optional<ExerciseSession> findByIdAndUserRoutine_UserId(Long id, Long userId);
}
