package com.gym.training.dto;

import com.gym.training.entity.Exercise.ExerciseType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    name = "ExerciseDTO",
    description = "Exercise details",
    example = "{\"id\": 1, \"name\": \"Bench Press\", \"description\": \"Chest exercise\", \"type\": \"STRENGTH\", \"disciplineId\": 1, \"disciplineName\": \"Chest\", \"createdBy\": 123, \"createdAt\": \"2026-03-21T10:30:00Z\", \"updatedAt\": \"2026-03-21T10:30:00Z\"}"
)
public class ExerciseDTO {
    @Schema(description = "Unique exercise identifier", example = "1")
    private Long id;

    @Schema(description = "Exercise name", example = "Bench Press")
    private String name;

    @Schema(description = "Exercise description and instructions", example = "Chest exercise")
    private String description;

    @Schema(description = "Type of exercise", example = "STRENGTH")
    private ExerciseType type;

    @Schema(description = "Associated discipline/category ID", example = "1")
    private Long disciplineId;

    @Schema(description = "Name of the associated discipline", example = "Chest")
    private String disciplineName;

    @Schema(description = "User ID who created the exercise", example = "123")
    private Long createdBy;

    @Schema(description = "Creation timestamp", example = "2026-03-21T10:30:00Z")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2026-03-21T10:30:00Z")
    private LocalDateTime updatedAt;
}
