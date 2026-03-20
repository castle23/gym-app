package com.gym.training.service;

import com.gym.common.dto.PageResponse;
import com.gym.training.dto.ExerciseSessionDTO;
import com.gym.training.dto.ExerciseSessionRequestDTO;
import com.gym.training.entity.Exercise;
import com.gym.training.entity.ExerciseSession;
import com.gym.training.entity.UserRoutine;
import com.gym.training.repository.ExerciseRepository;
import com.gym.training.repository.ExerciseSessionRepository;
import com.gym.training.repository.UserRoutineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseSessionService {
    
    private final ExerciseSessionRepository exerciseSessionRepository;
    private final UserRoutineRepository userRoutineRepository;
    private final ExerciseRepository exerciseRepository;
    
    /**
     * Get all sessions for a user routine with pagination
     */
    public PageResponse<ExerciseSessionDTO> getSessionsByRoutineId(Long userRoutineId, Pageable pageable) {
        log.info("Fetching sessions for routine: {} with pagination", userRoutineId);
        Page<ExerciseSession> page = exerciseSessionRepository.findByUserRoutineId(userRoutineId, pageable);
        return PageResponse.of(page.map(this::toDTO));
    }
    
    /**
     * Get sessions for a user on a specific date with pagination
     */
    public PageResponse<ExerciseSessionDTO> getSessionsByUserIdAndDate(Long userId, LocalDate date, Pageable pageable) {
        log.info("Fetching sessions for user: {} on date: {} with pagination", userId, date);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        Page<ExerciseSession> page = exerciseSessionRepository.findByUserIdAndDate(userId, startOfDay, endOfDay, pageable);
        return PageResponse.of(page.map(this::toDTO));
    }
    
    /**
     * Get session by ID with authorization check
     */
    public ExerciseSessionDTO getSessionById(Long sessionId, Long userId) {
        log.info("Fetching session: {} for user: {}", sessionId, userId);
        ExerciseSession session = exerciseSessionRepository.findByIdAndUserRoutine_UserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found or unauthorized"));
        return toDTO(session);
    }
    
    /**
     * Create a new exercise session
     */
    @Transactional
    public ExerciseSessionDTO createSession(ExerciseSessionRequestDTO request, Long userId) {
        log.info("Creating exercise session for user: {}", userId);
        
        UserRoutine userRoutine = userRoutineRepository.findById(request.getUserRoutineId())
                .orElseThrow(() -> new IllegalArgumentException("User routine not found: " + request.getUserRoutineId()));
        
        Exercise exercise = exerciseRepository.findById(request.getExerciseId())
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found: " + request.getExerciseId()));
        
        // Validate that the routine belongs to the user
        if (!userRoutine.getUserId().equals(userId)) {
            throw new IllegalArgumentException("User routine does not belong to user");
        }
        
        LocalDateTime sessionDate = request.getSessionDate() != null ? request.getSessionDate() : LocalDateTime.now();
        
        ExerciseSession session = ExerciseSession.builder()
                .userId(userId)
                .userRoutine(userRoutine)
                .exercise(exercise)
                .setsCompleted(request.getSets() != null ? request.getSets() : 0)
                .repsCompleted(request.getReps() != null ? request.getReps() : 0)
                .weightUsed(request.getWeight() != null ? BigDecimal.valueOf(request.getWeight()) : null)
                .durationSeconds(request.getDuration() != null ? (long) request.getDuration() : null)
                .notes(request.getNotes())
                .sessionDate(sessionDate)
                .build();
        
        ExerciseSession saved = exerciseSessionRepository.save(session);
        log.info("Exercise session created with ID: {}", saved.getId());
        return toDTO(saved);
    }
    
    /**
     * Update an exercise session
     */
    @Transactional
    public ExerciseSessionDTO updateSession(Long sessionId, ExerciseSessionRequestDTO request, Long userId) {
        log.info("Updating session: {} for user: {}", sessionId, userId);
        
        ExerciseSession session = exerciseSessionRepository.findByIdAndUserRoutine_UserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found or unauthorized"));
        
        if (request.getSets() != null) {
            session.setSetsCompleted(request.getSets());
        }
        if (request.getReps() != null) {
            session.setRepsCompleted(request.getReps());
        }
        if (request.getWeight() != null) {
            session.setWeightUsed(BigDecimal.valueOf(request.getWeight()));
        }
        if (request.getDuration() != null) {
            session.setDurationSeconds((long) request.getDuration());
        }
        if (request.getNotes() != null) {
            session.setNotes(request.getNotes());
        }
        
        ExerciseSession updated = exerciseSessionRepository.save(session);
        log.info("Session updated: {}", sessionId);
        return toDTO(updated);
    }
    
    /**
     * Delete an exercise session
     */
    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        log.info("Deleting session: {} for user: {}", sessionId, userId);
        
        ExerciseSession session = exerciseSessionRepository.findByIdAndUserRoutine_UserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found or unauthorized"));
        
        exerciseSessionRepository.delete(session);
        log.info("Session deleted: {}", sessionId);
    }
    
    /**
     * Convert ExerciseSession entity to DTO
     */
    private ExerciseSessionDTO toDTO(ExerciseSession session) {
        return ExerciseSessionDTO.builder()
                .id(session.getId())
                .userRoutineId(session.getUserRoutine().getId())
                .exerciseId(session.getExercise().getId())
                .exerciseName(session.getExercise().getName())
                .sets(session.getSetsCompleted())
                .reps(session.getRepsCompleted())
                .weight(session.getWeightUsed() != null ? session.getWeightUsed().doubleValue() : null)
                .duration(session.getDurationSeconds() != null ? session.getDurationSeconds().intValue() : null)
                .notes(session.getNotes())
                .sessionDate(session.getSessionDate())
                .createdAt(session.getCreatedAt())
                .build();
    }
}
