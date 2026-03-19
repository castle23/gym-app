package com.gym.training.dto;

import com.gym.training.entity.TemplateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutineTemplateRequestDTO {
    @NotBlank(message = "Template name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Template type is required")
    private TemplateType type;
    
    private List<Long> exerciseIds;
}
