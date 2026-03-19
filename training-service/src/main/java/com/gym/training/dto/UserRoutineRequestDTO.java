package com.gym.training.dto;

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
public class UserRoutineRequestDTO {
    @NotNull(message = "Routine template ID is required")
    private Long routineTemplateId;
    
    private LocalDateTime startDate;
    
    private LocalDateTime endDate;
    
    @Builder.Default
    private Boolean isActive = true;
}
