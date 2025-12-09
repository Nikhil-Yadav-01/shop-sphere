package com.rudraksha.shopsphere.batch.service;

import com.rudraksha.shopsphere.batch.dto.BatchJobResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface BatchJobService {

    void executeNightlyReportJob();

    void executeStockSyncJob();

    void executePriceSyncJob();

    Page<BatchJobResponse> getJobsByStatus(String status, Pageable pageable);

    List<BatchJobResponse> getJobsByName(String jobName);

    List<BatchJobResponse> getJobsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    List<BatchJobResponse> getFailedJobs();

    BatchJobResponse getJobById(Long jobId);

    void retryFailedJob(Long jobId);
}
