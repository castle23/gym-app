package com.gym.tracking.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "diet_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DietLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diet_component_id")
    private DietComponent dietComponent;

    @Column(nullable = false)
    private String mealType;

    @Column(nullable = false)
    private String foodItems;

    @Column(nullable = false)
    private Integer calories;
