package com.rudraksha.shopsphere.batch.repository;

import com.rudraksha.shopsphere.batch.entity.BatchJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BatchJobRepository extends JpaRepository<BatchJob, Long> {

    @Query("SELECT b FROM BatchJob b WHERE b.status = :status ORDER BY b.createdAt DESC")
    Page<BatchJob> findByStatus(@Param("status") BatchJob.JobStatus status, Pageable pageable);

    @Query("SELECT b FROM BatchJob b WHERE b.jobName = :jobName ORDER BY b.createdAt DESC")
    List<BatchJob> findByJobName(@Param("jobName") String jobName);

    @Query("SELECT b FROM BatchJob b WHERE b.createdAt BETWEEN :startDate AND :endDate ORDER BY b.createdAt DESC")
    List<BatchJob> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT b FROM BatchJob b WHERE b.status = 'FAILED' ORDER BY b.createdAt DESC")
    List<BatchJob> findFailedJobs();
}
