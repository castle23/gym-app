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
public class DietComponentDTO {
    private Long id;
    private Long planId;
    private String dietType;
    private Integer dailyCalories;
    private String macroDistribution;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
