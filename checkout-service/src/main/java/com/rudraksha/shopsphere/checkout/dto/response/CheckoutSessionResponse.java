package com.rudraksha.shopsphere.checkout.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutSessionResponse {
    private String sessionId;
    private String status;
    private String orderNumber;
    private BigDecimal totalAmount;
    private String failureReason;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
