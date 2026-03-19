package com.gym.training.repository;

import com.gym.training.entity.RoutineTemplate;
import com.gym.training.entity.TemplateType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoutineTemplateRepository extends JpaRepository<RoutineTemplate, Long> {
    Page<RoutineTemplate> findByType(TemplateType type, Pageable pageable);
    Page<RoutineTemplate> findByCreatedBy(Long userId, Pageable pageable);
    Optional<RoutineTemplate> findByIdAndCreatedBy(Long id, Long userId);
}

