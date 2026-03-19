package com.gym.training.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "disciplines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Discipline {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisciplineType type;

    public enum DisciplineType {
        STRENGTH, CARDIO, FLEXIBILITY, SPORTS, MIND_BODY, OTHER
    }
}
