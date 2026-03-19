package com.gym.training.repository;

import com.gym.training.entity.Exercise;
import com.gym.training.entity.Exercise.ExerciseType;
import com.gym.training.entity.Discipline;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    Page<Exercise> findByDiscipline(Discipline discipline, Pageable pageable);
    Page<Exercise> findByType(ExerciseType type, Pageable pageable);
    Page<Exercise> findByCreatedBy(Long userId, Pageable pageable);
    Optional<Exercise> findByIdAndCreatedBy(Long id, Long userId);
}

