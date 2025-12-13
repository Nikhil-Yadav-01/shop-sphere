package com.rudraksha.shopsphere.returns.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "returns")
public class Return {
    @Id
    private String id;

    @Indexed
    private String orderId;

    @Indexed
    private String customerId;

    private String reason;

    private String description;

    private List<String> itemIds;

    private BigDecimal refundAmount;

    @Indexed
    private ReturnStatus status;

    private String trackingNumber;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum ReturnStatus {
        INITIATED, APPROVED, REJECTED, IN_TRANSIT, RECEIVED, REFUNDED, CANCELLED
    }
}
