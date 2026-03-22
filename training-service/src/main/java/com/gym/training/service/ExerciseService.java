package com.gym.training.service;

import com.gym.common.dto.PageResponse;
import com.gym.training.dto.ExerciseDTO;
import com.gym.training.dto.ExerciseRequestDTO;
import com.gym.training.entity.Discipline;
import com.gym.training.entity.Exercise;
import com.gym.training.entity.Exercise.ExerciseType;
import com.gym.training.repository.DisciplineRepository;
import com.gym.training.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseService {
    
    private final ExerciseRepository exerciseRepository;
    private final DisciplineRepository disciplineRepository;
    
    /**
     * Get all system exercises with pagination
     */
    public PageResponse<ExerciseDTO> getAllSystemExercises(Pageable pageable) {
        log.info("Fetching all system exercises with pagination: page={}, size={}", 
                pageable.getPageNumber(), pageable.getPageSize());
        Page<Exercise> page = exerciseRepository.findByType(ExerciseType.SYSTEM, pageable);
        return PageResponse.of(page.map(this::toDTO));
    }
    
    /**
     * Get exercises by discipline with pagination
     */
    public PageResponse<ExerciseDTO> getExercisesByDiscipline(Long disciplineId, Pageable pageable) {
        log.info("Fetching exercises for discipline: {} with pagination", disciplineId);
        Discipline discipline = disciplineRepository.findById(disciplineId)
                .orElseThrow(() -> new IllegalArgumentException("Discipline not found: " + disciplineId));
        
        Page<Exercise> page = exerciseRepository.findByDiscipline(discipline, pageable);
        return PageResponse.of(page.map(this::toDTO));
    }
    
    /**
     * Search exercises by name and/or type with pagination.
     * Both parameters are optional; omitting them returns all exercises.
     */
    public PageResponse<ExerciseDTO> searchExercises(String name, ExerciseType type, Pageable pageable) {
        log.info("Searching exercises: name='{}', type={}, page={}, size={}",
                name, type, pageable.getPageNumber(), pageable.getPageSize());
        // Normalize blank string to null so the JPQL IS NULL check works correctly
        String normalizedName = (name != null && name.isBlank()) ? null : name;
        Page<Exercise> page = exerciseRepository.searchByNameAndType(normalizedName, type, pageable);
        return PageResponse.of(page.map(this::toDTO));
    }

    /**
     * Get exercises created by a user with pagination
     */
    public PageResponse<ExerciseDTO> getUserExercises(Long userId, Pageable pageable) {
        log.info("Fetching exercises created by user: {} with pagination", userId);
        Page<Exercise> page = exerciseRepository.findByCreatedBy(userId, pageable);
        return PageResponse.of(page.map(this::toDTO));
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
