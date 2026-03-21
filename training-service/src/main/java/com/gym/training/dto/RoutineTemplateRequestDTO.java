package com.gym.training.dto;

import com.gym.training.entity.RoutineTemplate.TemplateType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    name = "RoutineTemplateRequestDTO",
    description = "Request to create or update a routine template",
    example = "{\"name\": \"Weekly Upper Body\", \"description\": \"3-day upper body split\", \"type\": \"WEEKLY\", \"exerciseIds\": [1, 2, 3]}"
)
public class RoutineTemplateRequestDTO {
    @NotBlank(message = "Template name is required")
    @Schema(description = "Routine template name", example = "Weekly Upper Body")
    private String name;
    
    @Schema(description = "Routine description and instructions", example = "3-day upper body split")
    private String description;
    
    @NotNull(message = "Template type is required")
    @Schema(description = "Type of routine template", example = "WEEKLY", allowableValues = {"DAILY", "WEEKLY", "MONTHLY", "CUSTOM"})
    private TemplateType type;
    
    @Schema(description = "List of exercise IDs in this routine", example = "[1, 2, 3]")
    private List<Long> exerciseIds;
}
