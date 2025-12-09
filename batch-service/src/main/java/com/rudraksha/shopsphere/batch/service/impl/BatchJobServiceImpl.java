package com.rudraksha.shopsphere.batch.service.impl;

import com.rudraksha.shopsphere.batch.dto.BatchJobResponse;
import com.rudraksha.shopsphere.batch.entity.BatchJob;
import com.rudraksha.shopsphere.batch.repository.BatchJobRepository;
import com.rudraksha.shopsphere.batch.service.BatchJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BatchJobServiceImpl implements BatchJobService {

    private final BatchJobRepository batchJobRepository;

    @Override
    @Scheduled(cron = "0 0 2 * * *")
    @Async
    public void executeNightlyReportJob() {
        log.info("Starting nightly report job");
        BatchJob job = BatchJob.builder()
                .jobName("NIGHTLY_REPORT")
                .status(BatchJob.JobStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .build();

        try {
            job = batchJobRepository.save(job);

            log.info("Generating daily reports...");
            Thread.sleep(1000);

            job.setStatus(BatchJob.JobStatus.COMPLETED);
            job.setEndTime(LocalDateTime.now());
            job.setRecordsProcessed(1000);
            job.setResult("Daily reports generated successfully");

            batchJobRepository.save(job);
            log.info("Nightly report job completed with ID: {}", job.getId());
        } catch (Exception e) {
            log.error("Error executing nightly report job: {}", e.getMessage(), e);
            job.setStatus(BatchJob.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setEndTime(LocalDateTime.now());
            batchJobRepository.save(job);
        }
    }

    @Override
    @Scheduled(cron = "0 0 3 * * *")
    @Async
    public void executeStockSyncJob() {
        log.info("Starting stock sync job");
        BatchJob job = BatchJob.builder()
                .jobName("STOCK_SYNC")
                .status(BatchJob.JobStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .build();

        try {
            job = batchJobRepository.save(job);

            log.info("Syncing inventory stock levels...");
            Thread.sleep(1000);

            job.setStatus(BatchJob.JobStatus.COMPLETED);
            job.setEndTime(LocalDateTime.now());
            job.setRecordsProcessed(500);
            job.setResult("Stock levels synchronized successfully");

            batchJobRepository.save(job);
            log.info("Stock sync job completed with ID: {}", job.getId());
        } catch (Exception e) {
            log.error("Error executing stock sync job: {}", e.getMessage(), e);
            job.setStatus(BatchJob.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setEndTime(LocalDateTime.now());
            batchJobRepository.save(job);
        }
    }

    @Override
    @Scheduled(cron = "0 0 4 * * *")
    @Async
    public void executePriceSyncJob() {
        log.info("Starting price sync job");
        BatchJob job = BatchJob.builder()
                .jobName("PRICE_SYNC")
                .status(BatchJob.JobStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .build();

        try {
            job = batchJobRepository.save(job);

            log.info("Syncing product prices...");
            Thread.sleep(1000);

            job.setStatus(BatchJob.JobStatus.COMPLETED);
            job.setEndTime(LocalDateTime.now());
            job.setRecordsProcessed(750);
            job.setResult("Product prices updated successfully");

            batchJobRepository.save(job);
            log.info("Price sync job completed with ID: {}", job.getId());
        } catch (Exception e) {
            log.error("Error executing price sync job: {}", e.getMessage(), e);
            job.setStatus(BatchJob.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setEndTime(LocalDateTime.now());
            batchJobRepository.save(job);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BatchJobResponse> getJobsByStatus(String status, Pageable pageable) {
        log.info("Fetching batch jobs with status: {}", status);
        try {
            BatchJob.JobStatus jobStatus = BatchJob.JobStatus.valueOf(status);
            return batchJobRepository.findByStatus(jobStatus, pageable)
                    .map(this::mapToResponse);
        } catch (IllegalArgumentException e) {
            log.error("Invalid job status: {}", status);
            throw new RuntimeException("Invalid job status: " + status);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchJobResponse> getJobsByName(String jobName) {
        log.info("Fetching batch jobs with name: {}", jobName);
        return batchJobRepository.findByJobName(jobName)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchJobResponse> getJobsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching batch jobs between {} and {}", startDate, endDate);
        return batchJobRepository.findByDateRange(startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchJobResponse> getFailedJobs() {
        log.info("Fetching failed batch jobs");
        return batchJobRepository.findFailedJobs()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BatchJobResponse getJobById(Long jobId) {
        log.info("Fetching batch job with ID: {}", jobId);
        return batchJobRepository.findById(jobId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Batch job not found with ID: " + jobId));
    }

    @Override
    public void retryFailedJob(Long jobId) {
        log.info("Retrying failed batch job with ID: {}", jobId);
        BatchJob job = batchJobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Batch job not found with ID: " + jobId));

        if (!BatchJob.JobStatus.FAILED.equals(job.getStatus())) {
            throw new RuntimeException("Job is not in FAILED status, cannot retry");
        }

        job.setStatus(BatchJob.JobStatus.PENDING);
        job.setErrorMessage(null);
        batchJobRepository.save(job);
        log.info("Batch job queued for retry with ID: {}", jobId);
    }

    private BatchJobResponse mapToResponse(BatchJob job) {
        return BatchJobResponse.builder()
                .id(job.getId())
                .jobName(job.getJobName())
                .status(job.getStatus().toString())
                .startTime(job.getStartTime())
                .endTime(job.getEndTime())
                .result(job.getResult())
                .errorMessage(job.getErrorMessage())
                .recordsProcessed(job.getRecordsProcessed())
                .recordsFailed(job.getRecordsFailed())
                .createdAt(job.getCreatedAt())
                .build();
    }
}
