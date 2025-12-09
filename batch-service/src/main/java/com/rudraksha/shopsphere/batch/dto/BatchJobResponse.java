package com.rudraksha.shopsphere.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchJobResponse {
    private Long id;
    private String jobName;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String result;
    private String errorMessage;
    private Integer recordsProcessed;
    private Integer recordsFailed;
    private LocalDateTime createdAt;
}
