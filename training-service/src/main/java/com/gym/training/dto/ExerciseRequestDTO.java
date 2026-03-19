package com.gym.training.dto;

import com.gym.training.entity.Exercise.ExerciseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseRequestDTO {
    @NotBlank(message = "Exercise name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Exercise type is required")
    private ExerciseType type;
    
    @NotNull(message = "Discipline ID is required")
    private Long disciplineId;
}
