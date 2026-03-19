package com.gym.training.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseSessionRequestDTO {
    @NotNull(message = "User routine ID is required")
    private Long userRoutineId;
    
    @NotNull(message = "Exercise ID is required")
    private Long exerciseId;
    
    @Positive(message = "Sets must be greater than 0")
    private Integer sets;
    
    @Positive(message = "Reps must be greater than 0")
    private Integer reps;
    
    private Double weight;
    
    private Integer duration;
    
    private String notes;
    
    private LocalDateTime sessionDate;
}
