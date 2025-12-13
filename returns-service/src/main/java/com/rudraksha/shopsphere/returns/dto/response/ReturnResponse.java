package com.rudraksha.shopsphere.returns.dto.response;

import com.rudraksha.shopsphere.returns.entity.Return;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnResponse {
    private String id;
    private String orderId;
    private String customerId;
    private String reason;
    private String description;
    private List<String> itemIds;
    private BigDecimal refundAmount;
    private Return.ReturnStatus status;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
