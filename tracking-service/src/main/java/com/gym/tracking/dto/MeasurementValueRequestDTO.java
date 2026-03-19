package com.gym.tracking.dto;

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
public class MeasurementValueRequestDTO {
    @NotNull(message = "Measurement type ID is required")
    private Long measurementTypeId;

    @NotNull(message = "Value is required")
    @Positive(message = "Value must be positive")
    private Double value;

    @NotNull(message = "Measurement date is required")
    private LocalDate measurementDate;

    private String notes;
}
