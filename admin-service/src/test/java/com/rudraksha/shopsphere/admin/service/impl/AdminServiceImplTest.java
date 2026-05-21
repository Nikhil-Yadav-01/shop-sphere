package com.rudraksha.shopsphere.admin.service.impl;

import com.rudraksha.shopsphere.admin.entity.AdminAuditLog;
import com.rudraksha.shopsphere.admin.entity.SystemMetrics;
import com.rudraksha.shopsphere.admin.repository.AdminAuditLogRepository;
import com.rudraksha.shopsphere.admin.repository.SystemMetricsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private AdminAuditLogRepository auditLogRepository;
    @Mock
    private SystemMetricsRepository metricsRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void logAuditAction_Success() {
        adminService.logAuditAction(1L, "UPDATE", "PRODUCT", 123L, "Price changed", "127.0.0.1");

        verify(auditLogRepository).save(any(AdminAuditLog.class));
    }

    @Test
    void recordSystemMetric_Success() {
        adminService.recordSystemMetric("CPU_USAGE", 45.5, "PERCENT");

        verify(metricsRepository).save(any(SystemMetrics.class));
    }
}
