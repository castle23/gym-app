package com.gym.tracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    name = "PlanRequestDTO",
    description = "Request to create or update a training plan",
    example = "{\"name\": \"12-Week Muscle Gain\", \"description\": \"Hypertrophy focused plan\", \"objectiveId\": 1, \"status\": \"ACTIVE\", \"startDate\": \"2026-03-21T00:00:00\", \"endDate\": \"2026-06-21T00:00:00\"}"
)
public class PlanRequestDTO {
    @NotBlank(message = "Name is required")
    @Schema(description = "Plan name", example = "12-Week Muscle Gain")
    private String name;

    @NotBlank(message = "Description is required")
    @Schema(description = "Plan description and goals", example = "Hypertrophy focused plan")
    private String description;

    @NotNull(message = "Objective ID is required")
    @Schema(description = "Associated objective ID", example = "1")
    private Long objectiveId;

    @NotNull(message = "Status is required")
    @Schema(description = "Plan status", example = "ACTIVE", allowableValues = {"DRAFT", "ACTIVE", "PAUSED", "COMPLETED", "CANCELLED"})
    private String status;

    @NotNull(message = "Start date is required")
    @Schema(description = "Plan start date and time (ISO 8601)", example = "2026-03-21T00:00:00")
    private LocalDateTime startDate;

    @Schema(description = "Plan end date and time (ISO 8601)", example = "2026-06-21T00:00:00")
    private LocalDateTime endDate;
}
