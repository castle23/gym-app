package com.gym.training.service;

import com.gym.training.dto.ExerciseDTO;
import com.gym.training.dto.ExerciseRequestDTO;
import com.gym.training.entity.Discipline;
import com.gym.training.entity.Exercise;
import com.gym.training.entity.ExerciseType;
import com.gym.training.repository.DisciplineRepository;
import com.gym.training.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseService {
    
    private final ExerciseRepository exerciseRepository;
    private final DisciplineRepository disciplineRepository;
    
    /**
     * Get all system exercises
     */
    public List<ExerciseDTO> getAllSystemExercises() {
        log.info("Fetching all system exercises");
        return exerciseRepository.findByType(ExerciseType.SYSTEM)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get exercises by discipline
     */
    public List<ExerciseDTO> getExercisesByDiscipline(Long disciplineId) {
        log.info("Fetching exercises for discipline: {}", disciplineId);
        Discipline discipline = disciplineRepository.findById(disciplineId)
                .orElseThrow(() -> new IllegalArgumentException("Discipline not found: " + disciplineId));
        
        return exerciseRepository.findByDiscipline(discipline)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get exercises created by a user
     */
    public List<ExerciseDTO> getUserExercises(Long userId) {
        log.info("Fetching exercises created by user: {}", userId);
        return exerciseRepository.findByCreatedBy(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get exercise by ID
     */
    public ExerciseDTO getExerciseById(Long id) {
        log.info("Fetching exercise: {}", id);
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found: " + id));
        return toDTO(exercise);
    }
    
    /**
     * Create a new exercise
     */
    @Transactional
    public ExerciseDTO createExercise(ExerciseRequestDTO request, Long userId) {
        log.info("Creating exercise: {} by user: {}", request.getName(), userId);
        
        Discipline discipline = disciplineRepository.findById(request.getDisciplineId())
                .orElseThrow(() -> new IllegalArgumentException("Discipline not found: " + request.getDisciplineId()));
        
        Exercise exercise = Exercise.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .discipline(discipline)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        Exercise saved = exerciseRepository.save(exercise);
        log.info("Exercise created with ID: {}", saved.getId());
        return toDTO(saved);
    }
    
    /**
     * Update an exercise
     */
    @Transactional
    public ExerciseDTO updateExercise(Long id, ExerciseRequestDTO request, Long userId) {
        log.info("Updating exercise: {} by user: {}", id, userId);
        
        Exercise exercise = exerciseRepository.findByIdAndCreatedBy(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found or unauthorized"));
        
        Discipline discipline = disciplineRepository.findById(request.getDisciplineId())
                .orElseThrow(() -> new IllegalArgumentException("Discipline not found"));
        
        exercise.setName(request.getName());
        exercise.setDescription(request.getDescription());
        exercise.setType(request.getType());
        exercise.setDiscipline(discipline);
        exercise.setUpdatedAt(LocalDateTime.now());
        
        Exercise updated = exerciseRepository.save(exercise);
        log.info("Exercise updated: {}", id);
        return toDTO(updated);
    }
    
    /**
     * Delete an exercise
     */
    @Transactional
    public void deleteExercise(Long id, Long userId) {
        log.info("Deleting exercise: {} by user: {}", id, userId);
        
        Exercise exercise = exerciseRepository.findByIdAndCreatedBy(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found or unauthorized"));
        
        exerciseRepository.delete(exercise);
        log.info("Exercise deleted: {}", id);
    }
    
    /**
     * Convert Exercise entity to DTO
     */
    private ExerciseDTO toDTO(Exercise exercise) {
        return ExerciseDTO.builder()
                .id(exercise.getId())
                .name(exercise.getName())
                .description(exercise.getDescription())
                .type(exercise.getType())
                .disciplineId(exercise.getDiscipline().getId())
                .disciplineName(exercise.getDiscipline().getType().toString())
                .createdBy(exercise.getCreatedBy())
                .createdAt(exercise.getCreatedAt())
                .updatedAt(exercise.getUpdatedAt())
                .build();
    }
}
