package com.rudraksha.shopsphere.admin.repository;

import com.rudraksha.shopsphere.admin.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    @Query("SELECT a FROM AdminAuditLog a WHERE a.adminId = :adminId ORDER BY a.createdAt DESC")
    Page<AdminAuditLog> findByAdminId(@Param("adminId") Long adminId, Pageable pageable);

    @Query("SELECT a FROM AdminAuditLog a WHERE a.action = :action ORDER BY a.createdAt DESC")
    Page<AdminAuditLog> findByAction(@Param("action") String action, Pageable pageable);

    @Query("SELECT a FROM AdminAuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<AdminAuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM AdminAuditLog a WHERE a.resourceType = :resourceType AND a.resourceId = :resourceId ORDER BY a.createdAt DESC")
    List<AdminAuditLog> findByResource(@Param("resourceType") String resourceType, @Param("resourceId") Long resourceId);
}
