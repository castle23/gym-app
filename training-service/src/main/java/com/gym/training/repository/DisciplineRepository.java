package com.gym.training.repository;

import com.gym.training.entity.Discipline;
import com.gym.training.entity.Discipline.DisciplineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DisciplineRepository extends JpaRepository<Discipline, Long> {
    Optional<Discipline> findByType(DisciplineType type);
}
