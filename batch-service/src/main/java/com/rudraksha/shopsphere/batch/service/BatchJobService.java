package com.rudraksha.shopsphere.batch.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class BatchJobService {

    public Map<String, Object> executeJob(String jobName) {
        log.info("Executing batch job: {}", jobName);
        return Map.of(
            "success", true,
            "jobName", jobName,
            "status", "COMPLETED",
            "executedAt", LocalDateTime.now().toString()
        );
    }

    public List<String> getAvailableJobs() {
        return List.of("data-cleanup", "report-generation", "order-processing");
    }
}
