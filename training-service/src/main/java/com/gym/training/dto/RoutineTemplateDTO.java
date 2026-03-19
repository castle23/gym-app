package com.gym.training.dto;

import com.gym.training.entity.RoutineTemplate.TemplateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutineTemplateDTO {
    private Long id;
    private String name;
    private String description;
    private TemplateType type;
    private Long createdBy;
    private List<Long> exerciseIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
