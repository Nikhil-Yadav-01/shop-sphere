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
public class DashboardOverview {
    private Long totalAuditLogs;
    private Long totalMetricsRecorded;
    private String serviceStatus;
    private LocalDateTime lastUpdated;
}
