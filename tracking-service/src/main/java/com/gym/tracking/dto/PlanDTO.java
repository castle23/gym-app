package com.gym.tracking.dto;

import com.gym.tracking.entity.Plan;
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
    name = "PlanDTO",
    description = "Training plan details",
    example = "{\"id\": 1, \"userId\": 123, \"name\": \"12-Week Muscle Gain\", \"description\": \"Hypertrophy focused\", \"objectiveId\": 1, \"objectiveTitle\": \"Gain 10lbs Muscle\", \"status\": \"ACTIVE\", \"startDate\": \"2026-03-21T00:00:00\", \"endDate\": \"2026-06-21T00:00:00\", \"createdAt\": \"2026-03-21T10:30:00\", \"updatedAt\": \"2026-03-21T10:30:00\"}"
)
public class PlanDTO {
    @Schema(description = "Unique plan identifier", example = "1")
    private Long id;

    @Schema(description = "User ID who owns the plan", example = "123")
    private Long userId;

    @Schema(description = "Plan name", example = "12-Week Muscle Gain")
    private String name;

    @Schema(description = "Plan description", example = "Hypertrophy focused")
    private String description;

    @Schema(description = "Associated objective ID", example = "1")
    private Long objectiveId;

    @Schema(description = "Associated objective title", example = "Gain 10lbs Muscle")
    private String objectiveTitle;

    @Schema(description = "Current plan status", example = "ACTIVE")
    private Plan.PlanStatus status;

    @Schema(description = "Plan start date", example = "2026-03-21T00:00:00")
    private LocalDateTime startDate;

    @Schema(description = "Plan end date", example = "2026-06-21T00:00:00")
    private LocalDateTime endDate;

    @Schema(description = "Creation timestamp", example = "2026-03-21T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2026-03-21T10:30:00")
    private LocalDateTime updatedAt;
}
