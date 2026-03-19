package com.gym.tracking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasurementTypeRequestDTO {
    @NotBlank(message = "Type is required")
    private String type;

    @NotBlank(message = "Unit is required")
    private String unit;

    private Boolean isSystem;
}
