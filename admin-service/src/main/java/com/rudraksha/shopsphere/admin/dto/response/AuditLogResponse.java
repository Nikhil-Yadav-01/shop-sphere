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
public class AuditLogResponse {
    private Long id;
    private Long adminId;
    private String action;
    private String resourceType;
    private Long resourceId;
    private String changeDetails;
    private String ipAddress;
    private LocalDateTime createdAt;
}
