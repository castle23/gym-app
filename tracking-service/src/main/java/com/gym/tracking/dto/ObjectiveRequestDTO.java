package com.gym.tracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    name = "ObjectiveRequestDTO",
    description = "Request to create or update a fitness objective",
    example = "{\"title\": \"Bench Press 315 lbs\", \"description\": \"Achieve 315 lbs bench press\", \"category\": \"STRENGTH\", \"isActive\": true}"
)
public class ObjectiveRequestDTO {
    @NotBlank(message = "Title is required")
    @Schema(description = "Objective title", example = "Bench Press 315 lbs")
    private String title;

    @NotBlank(message = "Description is required")
    @Schema(description = "Objective description and details", example = "Achieve 315 lbs bench press")
    private String description;

    @NotBlank(message = "Category is required")
    @Schema(description = "Objective category", example = "STRENGTH", allowableValues = {"STRENGTH", "ENDURANCE", "FLEXIBILITY", "BODY_COMPOSITION", "SPEED"})
    private String category;

    @Schema(description = "Whether objective is currently active", example = "true")
    private Boolean isActive;
}
