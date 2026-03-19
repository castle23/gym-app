package com.gym.training.repository;

import com.gym.training.entity.Exercise;
import com.gym.training.entity.ExerciseType;
import com.gym.training.entity.Discipline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    List<Exercise> findByDiscipline(Discipline discipline);
    List<Exercise> findByType(ExerciseType type);
    List<Exercise> findByCreatedBy(Long userId);
    Optional<Exercise> findByIdAndCreatedBy(Long id, Long userId);
}
