package com.gym.training.service;

import com.gym.common.dto.PageResponse;
import com.gym.training.dto.RoutineTemplateDTO;
import com.gym.training.dto.RoutineTemplateRequestDTO;
import com.gym.training.entity.Exercise;
import com.gym.training.entity.RoutineTemplate;
import com.gym.training.entity.RoutineTemplate.TemplateType;
import com.gym.training.repository.ExerciseRepository;
import com.gym.training.repository.RoutineTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineTemplateService {
    
    private final RoutineTemplateRepository routineTemplateRepository;
    private final ExerciseRepository exerciseRepository;
    
    /**
     * Get all system templates with pagination
     */
    public PageResponse<RoutineTemplateDTO> getAllSystemTemplates(Pageable pageable) {
        log.info("Fetching all system routine templates with pagination: page={}, size={}", 
                pageable.getPageNumber(), pageable.getPageSize());
        Page<RoutineTemplate> page = routineTemplateRepository.findByType(TemplateType.SYSTEM, pageable);
        return PageResponse.of(page.map(this::toDTO));
    }
    
    /**
     * Get templates created by a user with pagination
     */
    public PageResponse<RoutineTemplateDTO> getUserTemplates(Long userId, Pageable pageable) {
        log.info("Fetching routine templates created by user: {} with pagination", userId);
        Page<RoutineTemplate> page = routineTemplateRepository.findByCreatedBy(userId, pageable);
        return PageResponse.of(page.map(this::toDTO));
    }
    
    /**
     * Get template by ID
     */
    public RoutineTemplateDTO getTemplateById(Long id) {
        log.info("Fetching routine template: {}", id);
        RoutineTemplate template = routineTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Routine template not found: " + id));
        return toDTO(template);
    }
    
    /**
     * Create a new routine template
     */
    @Transactional
    public RoutineTemplateDTO createTemplate(RoutineTemplateRequestDTO request, Long userId) {
        log.info("Creating routine template: {} by user: {}", request.getName(), userId);
        
        List<Exercise> exercises = exerciseRepository.findAllById(request.getExerciseIds() != null ? request.getExerciseIds() : List.of());
        
        RoutineTemplate template = RoutineTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .exercises(exercises)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        RoutineTemplate saved = routineTemplateRepository.save(template);
        log.info("Routine template created with ID: {}", saved.getId());
        return toDTO(saved);
    }
    
    /**
     * Update a routine template
     */
    @Transactional
    public RoutineTemplateDTO updateTemplate(Long id, RoutineTemplateRequestDTO request, Long userId) {
        log.info("Updating routine template: {} by user: {}", id, userId);
        
        RoutineTemplate template = routineTemplateRepository.findByIdAndCreatedBy(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Routine template not found or unauthorized"));
        
        List<Exercise> exercises = exerciseRepository.findAllById(request.getExerciseIds() != null ? request.getExerciseIds() : List.of());
        
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setType(request.getType());
        template.setExercises(exercises);
        template.setUpdatedAt(LocalDateTime.now());
        
        RoutineTemplate updated = routineTemplateRepository.save(template);
        log.info("Routine template updated: {}", id);
        return toDTO(updated);
    }
    
    /**
     * Delete a routine template
     */
    @Transactional
    public void deleteTemplate(Long id, Long userId) {
        log.info("Deleting routine template: {} by user: {}", id, userId);
        
        RoutineTemplate template = routineTemplateRepository.findByIdAndCreatedBy(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Routine template not found or unauthorized"));
        
        routineTemplateRepository.delete(template);
        log.info("Routine template deleted: {}", id);
    }
    
    /**
     * Convert RoutineTemplate entity to DTO
     */
    private RoutineTemplateDTO toDTO(RoutineTemplate template) {
        return RoutineTemplateDTO.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .type(template.getType())
                .createdBy(template.getCreatedBy())
                .exerciseIds(template.getExercises().stream()
                        .map(Exercise::getId)
                        .collect(Collectors.toList()))
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
