package com.gym.tracking.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
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
    private LocalDate logDate;

    @Column(nullable = false)
    private String meal;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String foodItems;

    @Column(nullable = false)
    private Double calories;

    @Column
    private String macros;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
