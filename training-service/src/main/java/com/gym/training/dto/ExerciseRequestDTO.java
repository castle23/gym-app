package com.gym.training.dto;

import com.gym.training.entity.Exercise.ExerciseType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(
    name = "ExerciseRequestDTO",
    description = "Request to create or update an exercise",
    example = "{\"name\": \"Bench Press\", \"description\": \"Chest and tricep compound exercise\", \"type\": \"STRENGTH\", \"disciplineId\": 1}"
)
public class ExerciseRequestDTO {
    @NotBlank(message = "Exercise name is required")
    @Schema(description = "Exercise name", example = "Bench Press")
    private String name;
    
    @Schema(description = "Exercise description and instructions", example = "Chest and tricep compound exercise")
    private String description;
    
    @NotNull(message = "Exercise type is required")
    @Schema(description = "Type of exercise", example = "STRENGTH", allowableValues = {"STRENGTH", "CARDIO", "FLEXIBILITY", "BALANCE"})
    private ExerciseType type;
    
    @NotNull(message = "Discipline ID is required")
    @Schema(description = "Associated discipline/category ID", example = "1")
    private Long disciplineId;
}
