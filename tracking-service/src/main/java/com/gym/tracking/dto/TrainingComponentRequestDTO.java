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
public class TrainingComponentRequestDTO {
    @NotNull(message = "Plan ID is required")
    private Long planId;

    @NotBlank(message = "Focus is required")
    private String focus;

    @NotBlank(message = "Intensity is required")
    private String intensity;

    @NotNull(message = "Frequency per week is required")
    @Positive(message = "Frequency must be positive")
    private Integer frequencyPerWeek;
}
