package com.rudraksha.shopsphere.checkout.service;

import com.rudraksha.shopsphere.checkout.entity.OutboxEvent;
import com.rudraksha.shopsphere.checkout.repository.OutboxEventRepository;
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

    @Value("${checkout.outbox.max-retries:5}")
    private int maxRetries;

    @Value("${checkout.outbox.batch-size:100}")
    private int batchSize;

    @Value("${checkout.outbox.cleanup-days:7}")
    private int cleanupDays;

    /**
     * Polls the checkout_outbox_events table for unprocessed events and publishes them to Kafka.
     * Uses SKIP LOCKED to allow safe concurrent execution across multiple instances.
     */
    @Scheduled(fixedDelayString = "${checkout.outbox.poll-interval-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository.findUnprocessedForPublishing(batchSize, maxRetries);
        if (events.isEmpty()) {
            return;
        }
        log.debug("Publishing {} checkout outbox events", events.size());

        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);
                event.setProcessed(true);
                log.info("Checkout outbox event published: id={}, type={}, topic={}",
                        event.getId(), event.getEventType(), event.getTopic());
            } catch (Exception e) {
                int nextRetry = event.getRetryCount() + 1;
                event.setRetryCount(nextRetry);
                String errMsg = e.getMessage();
                if (errMsg != null && errMsg.length() > 500) {
                    errMsg = errMsg.substring(0, 500);
                }
                event.setLastError(errMsg);

                if (nextRetry >= maxRetries) {
                    log.error("Checkout outbox event {} (type={}) exceeded max retries and is stuck. Last error: {}",
                            event.getId(), event.getEventType(), errMsg);
                } else {
                    log.warn("Failed to publish checkout outbox event {} (retry {}/{}): {}",
                            event.getId(), nextRetry, maxRetries, errMsg);
                }
            }
        }
        outboxRepository.saveAll(events);
    }

    /**
     * Nightly cleanup of processed outbox events older than configured retention days.
     */
    @Scheduled(cron = "${checkout.outbox.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void cleanupProcessedEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(cleanupDays);
        log.info("Cleaning up checkout outbox events older than {}", threshold);
        try {
            int deleted = outboxRepository.deleteProcessedBefore(threshold);
            log.info("Deleted {} processed checkout outbox events", deleted);
        } catch (Exception e) {
            log.error("Failed to cleanup checkout outbox events", e);
        }
    }
}
