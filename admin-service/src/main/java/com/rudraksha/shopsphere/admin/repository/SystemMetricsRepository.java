package com.rudraksha.shopsphere.admin.repository;

import com.rudraksha.shopsphere.admin.entity.SystemMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemMetricsRepository extends JpaRepository<SystemMetrics, Long> {

    @Query("SELECT s FROM SystemMetrics s WHERE s.metricType = :metricType AND s.recordedAt BETWEEN :startTime AND :endTime ORDER BY s.recordedAt DESC")
    List<SystemMetrics> findMetricsByType(@Param("metricType") String metricType, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT s FROM SystemMetrics s WHERE s.recordedAt >= :since ORDER BY s.recordedAt DESC")
    List<SystemMetrics> findRecentMetrics(@Param("since") LocalDateTime since);
}
