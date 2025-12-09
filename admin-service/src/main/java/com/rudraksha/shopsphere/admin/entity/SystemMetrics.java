package com.rudraksha.shopsphere.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_metrics", indexes = {
        @Index(name = "idx_metric_type", columnList = "metric_type"),
        @Index(name = "idx_recorded_at", columnList = "recorded_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String metricType; // CPU, MEMORY, REQUEST_COUNT, etc.

    @Column(nullable = false)
    private Double value;

    @Column(nullable = false)
    private String unit; // %, MB, COUNT, etc.

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        this.recordedAt = LocalDateTime.now();
    }
}
