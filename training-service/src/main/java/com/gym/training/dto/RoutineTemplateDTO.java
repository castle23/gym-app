package com.gym.training.dto;

import com.gym.training.entity.RoutineTemplate.TemplateType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    name = "RoutineTemplateDTO",
    description = "Routine template details",
    example = "{\"id\": 1, \"name\": \"Weekly Upper Body\", \"description\": \"3-day upper body split\", \"type\": \"WEEKLY\", \"createdBy\": 123, \"exerciseIds\": [1, 2, 3], \"createdAt\": \"2026-03-21T10:30:00Z\", \"updatedAt\": \"2026-03-21T10:30:00Z\"}"
)
public class RoutineTemplateDTO {
    @Schema(description = "Unique routine template identifier", example = "1")
    private Long id;

    @Schema(description = "Routine template name", example = "Weekly Upper Body")
    private String name;

    @Schema(description = "Routine description", example = "3-day upper body split")
    private String description;

    @Schema(description = "Template type", example = "WEEKLY")
    private TemplateType type;

    @Schema(description = "User ID who created the template", example = "123")
    private Long createdBy;

    @Schema(description = "List of exercise IDs in this routine", example = "[1, 2, 3]")
    private List<Long> exerciseIds;

    @Schema(description = "Creation timestamp", example = "2026-03-21T10:30:00Z")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2026-03-21T10:30:00Z")
    private LocalDateTime updatedAt;
}
