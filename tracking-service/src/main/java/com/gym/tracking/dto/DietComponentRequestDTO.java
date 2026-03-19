package com.gym.tracking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DietComponentRequestDTO {
    @NotNull(message = "Plan ID is required")
    private Long planId;

    @NotBlank(message = "Diet type is required")
    private String dietType;

    @NotNull(message = "Daily calories is required")
    @Positive(message = "Daily calories must be positive")
    private Integer dailyCalories;

    @NotBlank(message = "Macro distribution is required")
    private String macroDistribution;
}
