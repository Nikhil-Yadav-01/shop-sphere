package com.rudraksha.shopsphere.batch.controller;

import com.rudraksha.shopsphere.batch.dto.BatchJobResponse;
import com.rudraksha.shopsphere.batch.service.BatchJobService;
import com.rudraksha.shopsphere.batch.models.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchController {

    private final BatchJobService batchJobService;

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<Page<BatchJobResponse>>> getJobsByStatus(
            @RequestParam String status,
            Pageable pageable) {
        log.info("Get batch jobs by status request - status: {}", status);
        Page<BatchJobResponse> jobs = batchJobService.getJobsByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @GetMapping("/jobs/name/{jobName}")
    public ResponseEntity<ApiResponse<List<BatchJobResponse>>> getJobsByName(
            @PathVariable String jobName) {
        log.info("Get batch jobs by name request - jobName: {}", jobName);
        List<BatchJobResponse> jobs = batchJobService.getJobsByName(jobName);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @GetMapping("/jobs/date-range")
    public ResponseEntity<ApiResponse<List<BatchJobResponse>>> getJobsByDateRange(
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {
        log.info("Get batch jobs by date range - {} to {}", startDate, endDate);
        List<BatchJobResponse> jobs = batchJobService.getJobsByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @GetMapping("/jobs/failed")
    public ResponseEntity<ApiResponse<List<BatchJobResponse>>> getFailedJobs() {
        log.info("Get failed batch jobs request");
        List<BatchJobResponse> jobs = batchJobService.getFailedJobs();
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ApiResponse<BatchJobResponse>> getJobById(@PathVariable Long jobId) {
        log.info("Get batch job request - jobId: {}", jobId);
        BatchJobResponse job = batchJobService.getJobById(jobId);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @PostMapping("/jobs/{jobId}/retry")
    public ResponseEntity<ApiResponse<String>> retryJob(@PathVariable Long jobId) {
        log.info("Retry batch job request - jobId: {}", jobId);
        batchJobService.retryFailedJob(jobId);
        return ResponseEntity.ok(ApiResponse.success("Job queued for retry"));
    }

    @PostMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Batch Service is running"));
    }
}
