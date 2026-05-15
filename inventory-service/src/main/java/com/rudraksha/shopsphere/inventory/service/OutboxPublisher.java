package com.rudraksha.shopsphere.inventory.service;

import com.rudraksha.shopsphere.inventory.entity.OutboxEvent;
import com.rudraksha.shopsphere.inventory.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {
    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishEvents() {
        List<OutboxEvent> events = outboxRepository.findByProcessedFalseOrderByCreatedAtAsc();
        if (events.isEmpty()) return;

        log.info("Publishing {} outbox events", events.size());
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getKey(), event.getPayload()).get();
                event.setProcessed(true);
                outboxRepository.save(event);
            } catch (Exception e) {
                log.error("Failed to publish outbox event: {}", event.getId(), e);
            }
        }
    }
}
