package com.rudraksha.shopsphere.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemMetricsResponse {
    private Long id;
    private String metricType;
    private Double value;
    private String unit;
    private LocalDateTime recordedAt;
}
