package com.gym.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DietLogDTO {
    private Long id;
    private Long userId;
    private LocalDate logDate;
    private String meal;
    private String foodItems;
    private Double calories;
    private String macros;
    private String notes;
    private LocalDateTime createdAt;
}
