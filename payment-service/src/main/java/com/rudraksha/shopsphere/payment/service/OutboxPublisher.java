package com.rudraksha.shopsphere.payment.service;

import com.rudraksha.shopsphere.payment.entity.OutboxEvent;
import com.rudraksha.shopsphere.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${outbox.scheduler.batch-size:100}")
    private int batchSize;

    @Value("${outbox.scheduler.max-retries:5}")
    private int maxRetries;

    @Value("${outbox.scheduler.cleanup-days:7}")
    private int cleanupDays;

    @Scheduled(fixedDelayString = "${outbox.scheduler.interval-ms:1000}")
    @Transactional
    public void publishEvents() {
        // Fetch a batch of unprocessed events with a pessimistic lock, skipping already locked ones
        List<OutboxEvent> events = outboxRepository.findUnprocessedForPublishing(batchSize, maxRetries);
        if (events.isEmpty()) {
            return;
        }

        log.debug("Found {} payment outbox events to publish", events.size());
        
        for (OutboxEvent event : events) {
            try {
                // Synchronous send to ensure event delivery before marking as processed
                kafkaTemplate.send(event.getTopic(), event.getKey(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);
                
                event.setProcessed(true);
                log.info("Successfully published payment outbox event: {} to topic {}", event.getId(), event.getTopic());
            } catch (Exception e) {
                int currentRetry = event.getRetryCount() + 1;
                event.setRetryCount(currentRetry);
                
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.length() > 500) {
                    errorMsg = errorMsg.substring(0, 500); // Truncate long error messages
                }
                event.setLastError(errorMsg);
                
                if (currentRetry >= maxRetries) {
                    log.error("Payment outbox event {} exceeded max retries ({}) and is now marked as FAILED.", 
                            event.getId(), maxRetries, e);
                } else {
                    log.warn("Failed to publish payment outbox event {} (Retry {}/{}): {}", 
                            event.getId(), currentRetry, maxRetries, e.getMessage());
                }
            }
        }
        
        outboxRepository.saveAll(events);
    }

    @Scheduled(cron = "${outbox.scheduler.cleanup-cron:0 0 2 * * *}") // Run daily at 2:00 AM
    public void cleanupProcessedEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(cleanupDays);
        log.info("Starting cleanup of processed payment outbox events older than {}", threshold);
        try {
            int deletedCount = outboxRepository.deleteProcessedBefore(threshold);
            log.info("Successfully deleted {} processed payment outbox events", deletedCount);
        } catch (Exception e) {
            log.error("Failed to clean up processed payment outbox events", e);
        }
    }
}
