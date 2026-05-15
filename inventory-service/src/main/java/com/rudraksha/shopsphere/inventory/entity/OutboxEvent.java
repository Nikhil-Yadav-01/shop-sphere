package com.rudraksha.shopsphere.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private String key;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(nullable = false)
    private boolean processed = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
