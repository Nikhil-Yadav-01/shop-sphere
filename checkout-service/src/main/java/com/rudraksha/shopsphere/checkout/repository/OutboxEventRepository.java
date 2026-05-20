package com.rudraksha.shopsphere.checkout.repository;

import com.rudraksha.shopsphere.checkout.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(value = "SELECT * FROM checkout_outbox_events WHERE processed = false AND retry_count < :maxRetries ORDER BY created_at ASC LIMIT :limit FOR UPDATE SKIP LOCKED",
           nativeQuery = true)
    List<OutboxEvent> findUnprocessedForPublishing(@Param("limit") int limit, @Param("maxRetries") int maxRetries);

    @Modifying
    @Transactional
    @Query("DELETE FROM OutboxEvent e WHERE e.processed = true AND e.createdAt < :threshold")
    int deleteProcessedBefore(@Param("threshold") LocalDateTime threshold);
}
