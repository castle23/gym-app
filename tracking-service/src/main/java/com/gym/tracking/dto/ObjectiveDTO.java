package com.gym.tracking.dto;

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
    name = "ObjectiveDTO",
    description = "Fitness objective details",
    example = "{\"id\": 1, \"userId\": 123, \"title\": \"Bench Press 315 lbs\", \"description\": \"Achieve 315 lbs\", \"category\": \"STRENGTH\", \"isActive\": true, \"createdAt\": \"2026-03-21T10:30:00\", \"updatedAt\": \"2026-03-21T10:30:00\"}"
)
public class ObjectiveDTO {
    @Schema(description = "Unique objective identifier", example = "1")
    private Long id;

    @Schema(description = "User ID who owns the objective", example = "123")
    private Long userId;

    @Schema(description = "Objective title", example = "Bench Press 315 lbs")
    private String title;

    @Schema(description = "Objective description", example = "Achieve 315 lbs")
    private String description;

    @Schema(description = "Objective category", example = "STRENGTH")
    private String category;

    @Schema(description = "Whether objective is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Creation timestamp", example = "2026-03-21T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2026-03-21T10:30:00")
    private LocalDateTime updatedAt;
}
