package com.gym.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingComponentDTO {
    private Long id;
    private Long planId;
    private String focus;
    private String intensity;
    private Integer frequencyPerWeek;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
