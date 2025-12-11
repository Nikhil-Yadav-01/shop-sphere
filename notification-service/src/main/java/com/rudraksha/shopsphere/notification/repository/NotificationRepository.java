package com.rudraksha.shopsphere.notification.repository;

import com.rudraksha.shopsphere.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserId(String userId);

    List<Notification> findByUserIdAndIsRead(String userId, Boolean isRead);

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(@Param("userId") String userId, @Param("since") LocalDateTime since);

    List<Notification> findByStatus(Notification.NotificationStatus status);

    List<Notification> findByStatusAndCreatedAtBefore(Notification.NotificationStatus status, LocalDateTime before);

    long countByUserIdAndIsRead(String userId, Boolean isRead);
}
