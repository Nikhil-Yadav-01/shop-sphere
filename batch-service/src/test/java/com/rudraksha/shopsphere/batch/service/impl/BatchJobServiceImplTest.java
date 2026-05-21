package com.rudraksha.shopsphere.batch.service.impl;

import com.rudraksha.shopsphere.batch.dto.BatchJobResponse;
import com.rudraksha.shopsphere.batch.entity.BatchJob;
import com.rudraksha.shopsphere.batch.repository.BatchJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchJobServiceImplTest {

    @Mock
    private BatchJobRepository batchJobRepository;

    @InjectMocks
    private BatchJobServiceImpl batchJobService;

    private BatchJob batchJob;
    private Long jobId = 1L;

    @BeforeEach
    void setUp() {
        batchJob = BatchJob.builder()
                .id(jobId)
                .jobName("NIGHTLY_REPORT")
                .status(BatchJob.JobStatus.FAILED)
                .build();
    }

    @Test
    void getJobById_Success() {
        when(batchJobRepository.findById(jobId)).thenReturn(Optional.of(batchJob));

        BatchJobResponse response = batchJobService.getJobById(jobId);

        assertNotNull(response);
        assertEquals(jobId, response.getId());
    }

    @Test
    void retryFailedJob_Success() {
        when(batchJobRepository.findById(jobId)).thenReturn(Optional.of(batchJob));
        when(batchJobRepository.save(any(BatchJob.class))).thenReturn(batchJob);

        assertDoesNotThrow(() -> batchJobService.retryFailedJob(jobId));

        assertEquals(BatchJob.JobStatus.PENDING, batchJob.getStatus());
        assertNull(batchJob.getErrorMessage());
        verify(batchJobRepository).save(batchJob);
    }

    @Test
    void retryFailedJob_NotFailedStatus() {
        batchJob.setStatus(BatchJob.JobStatus.COMPLETED);
        when(batchJobRepository.findById(jobId)).thenReturn(Optional.of(batchJob));

        assertThrows(RuntimeException.class, () -> batchJobService.retryFailedJob(jobId));
    }
}
