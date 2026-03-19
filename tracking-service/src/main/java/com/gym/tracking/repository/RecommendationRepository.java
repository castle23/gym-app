package com.gym.tracking.repository;

import com.gym.tracking.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByTrainingComponentId(Long trainingComponentId);
    List<Recommendation> findByDietComponentId(Long dietComponentId);
}
