package com.rudraksha.shopsphere.admin.service;

import com.rudraksha.shopsphere.admin.dto.response.AuditLogResponse;
import com.rudraksha.shopsphere.admin.dto.response.SystemMetricsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminService {

    void logAuditAction(Long adminId, String action, String resourceType, Long resourceId, String details, String ipAddress);

    Page<AuditLogResponse> getAuditLogs(Long adminId, Pageable pageable);

    Page<AuditLogResponse> getAuditLogsByAction(String action, Pageable pageable);

    List<AuditLogResponse> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    List<AuditLogResponse> getAuditLogsByResource(String resourceType, Long resourceId);

    void recordSystemMetric(String metricType, Double value, String unit);

    List<SystemMetricsResponse> getSystemMetrics(String metricType, LocalDateTime startTime, LocalDateTime endTime);

    List<SystemMetricsResponse> getRecentMetrics(LocalDateTime since);
}
