package com.gym.training.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseSessionDTO {
    private Long id;
    private Long userRoutineId;
    private Long exerciseId;
    private String exerciseName;
    private Integer sets;
    private Integer reps;
    private Double weight;
    private Integer duration;
    private String notes;
    private LocalDateTime sessionDate;
    private LocalDateTime createdAt;
}
