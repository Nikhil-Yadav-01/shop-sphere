package com.rudraksha.shopsphere.batch.controller;

import com.rudraksha.shopsphere.batch.service.BatchJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchController {

    private final BatchJobService batchJobService;

    @GetMapping("/")
    public String welcome() {
        return "Welcome to Batch Service";
    }

    @PostMapping("/jobs/{jobName}/execute")
    public ResponseEntity<Map<String, Object>> executeJob(@PathVariable String jobName) {
        return ResponseEntity.ok(batchJobService.executeJob(jobName));
    }

    @GetMapping("/jobs")
    public ResponseEntity<Map<String, Object>> listJobs() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "jobs", batchJobService.getAvailableJobs()
        ));
    }
}
