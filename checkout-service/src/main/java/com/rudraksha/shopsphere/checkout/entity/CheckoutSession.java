package com.rudraksha.shopsphere.checkout.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "checkout_sessions",
    indexes = {
        @Index(name = "idx_cs_user_id", columnList = "user_id"),
        @Index(name = "idx_cs_order_number", columnList = "order_number"),
        @Index(name = "idx_cs_idempotency_key", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_cs_session_id", columnList = "session_id", unique = true)
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 36)
    private String sessionId; // UUID string

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "order_number")
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CheckoutStatus status;

    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum CheckoutStatus {
        PENDING,
        ORDER_CREATED,
        INVENTORY_RESERVED,
        PAYMENT_PROCESSING,
        COMPLETED,
        FAILED
    }
}
