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
public class MeasurementValueDTO {
    private Long id;
    private Long userId;
    private Long measurementTypeId;
    private String measurementType;
    private Double value;
    private LocalDate measurementDate;
    private String notes;
    private LocalDateTime createdAt;
}
