package com.rudraksha.shopsphere.batch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "batch_job", indexes = {
        @Index(name = "idx_job_name", columnList = "job_name"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String jobName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private Integer recordsProcessed = 0;

    @Column(nullable = false)
    private Integer recordsFailed = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = JobStatus.PENDING;
        }
    }

    public enum JobStatus {
        PENDING, RUNNING, COMPLETED, FAILED
    }
}
