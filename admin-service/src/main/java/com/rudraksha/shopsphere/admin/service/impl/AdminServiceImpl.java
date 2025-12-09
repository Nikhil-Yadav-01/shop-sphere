package com.rudraksha.shopsphere.admin.service.impl;

import com.rudraksha.shopsphere.admin.dto.response.AuditLogResponse;
import com.rudraksha.shopsphere.admin.dto.response.SystemMetricsResponse;
import com.rudraksha.shopsphere.admin.entity.AdminAuditLog;
import com.rudraksha.shopsphere.admin.entity.SystemMetrics;
import com.rudraksha.shopsphere.admin.repository.AdminAuditLogRepository;
import com.rudraksha.shopsphere.admin.repository.SystemMetricsRepository;
import com.rudraksha.shopsphere.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminServiceImpl implements AdminService {

    private final AdminAuditLogRepository auditLogRepository;
    private final SystemMetricsRepository metricsRepository;

    @Override
    public void logAuditAction(Long adminId, String action, String resourceType, Long resourceId, String details, String ipAddress) {
        log.info("Logging audit action - Admin: {}, Action: {}, Resource: {}/{}", adminId, action, resourceType, resourceId);

        AdminAuditLog adminAuditLog = AdminAuditLog.builder()
                .adminId(adminId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .changeDetails(details)
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(adminAuditLog);
        log.info("Audit log saved successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(Long adminId, Pageable pageable) {
        log.info("Fetching audit logs for admin: {}", adminId);
        return auditLogRepository.findByAdminId(adminId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByAction(String action, Pageable pageable) {
        log.info("Fetching audit logs for action: {}", action);
        return auditLogRepository.findByAction(action, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching audit logs between {} and {}", startDate, endDate);
        return auditLogRepository.findByDateRange(startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogsByResource(String resourceType, Long resourceId) {
        log.info("Fetching audit logs for resource: {}/{}", resourceType, resourceId);
        return auditLogRepository.findByResource(resourceType, resourceId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void recordSystemMetric(String metricType, Double value, String unit) {
        log.info("Recording system metric - Type: {}, Value: {} {}", metricType, value, unit);

        SystemMetrics metric = SystemMetrics.builder()
                .metricType(metricType)
                .value(value)
                .unit(unit)
                .build();

        metricsRepository.save(metric);
        log.info("System metric recorded successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemMetricsResponse> getSystemMetrics(String metricType, LocalDateTime startTime, LocalDateTime endTime) {
        log.info("Fetching metrics - Type: {} between {} and {}", metricType, startTime, endTime);
        return metricsRepository.findMetricsByType(metricType, startTime, endTime)
                .stream()
                .map(this::mapMetricsToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemMetricsResponse> getRecentMetrics(LocalDateTime since) {
        log.info("Fetching recent metrics since: {}", since);
        return metricsRepository.findRecentMetrics(since)
                .stream()
                .map(this::mapMetricsToResponse)
                .collect(Collectors.toList());
    }

    private AuditLogResponse mapToResponse(AdminAuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .adminId(log.getAdminId())
                .action(log.getAction())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .changeDetails(log.getChangeDetails())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private SystemMetricsResponse mapMetricsToResponse(SystemMetrics metric) {
        return SystemMetricsResponse.builder()
                .id(metric.getId())
                .metricType(metric.getMetricType())
                .value(metric.getValue())
                .unit(metric.getUnit())
                .recordedAt(metric.getRecordedAt())
                .build();
    }
}
