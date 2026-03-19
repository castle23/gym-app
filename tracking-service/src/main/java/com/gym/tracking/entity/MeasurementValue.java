package com.gym.tracking.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "measurement_values")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "measurement_type_id", nullable = false)
    private MeasurementType measurementType;

    @Column(nullable = false)
    private Double value;

    @Column
    private String notes;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
