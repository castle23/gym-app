package com.gym.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProfessionalRegistrationDto", description = "Details for professional registration request")
public class ProfessionalRegistrationDto {
    @NotBlank
    @Schema(description = "Specialty (e.g., Personal Trainer, Nutritionist)", example = "Personal Trainer")
    private String specialty;

    @NotBlank
    @Schema(description = "Professional license number", example = "LIC123456")
    private String licenseNumber;
}
