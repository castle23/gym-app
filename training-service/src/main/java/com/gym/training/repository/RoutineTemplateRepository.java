package com.gym.training.repository;

import com.gym.training.entity.RoutineTemplate;
import com.gym.training.entity.TemplateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoutineTemplateRepository extends JpaRepository<RoutineTemplate, Long> {
    List<RoutineTemplate> findByType(TemplateType type);
    List<RoutineTemplate> findByCreatedBy(Long userId);
    Optional<RoutineTemplate> findByIdAndCreatedBy(Long id, Long userId);
}
