package com.gym.tracking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DietLogRequestDTO {
    @NotNull(message = "Log date is required")
    private LocalDate logDate;

    @NotBlank(message = "Meal is required")
    private String meal;

    @NotBlank(message = "Food items are required")
    private String foodItems;

    @NotNull(message = "Calories are required")
    @Positive(message = "Calories must be positive")
    private Double calories;

    private String macros;
    private String notes;
}
