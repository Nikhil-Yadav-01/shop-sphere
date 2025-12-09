package com.rudraksha.shopsphere.shared.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, event).get();
            log.info("Event published to topic={} with key={}", topic, key);
        } catch (Exception e) {
            log.error("Failed to publish event to topic={} with key={}", topic, key, e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    public CompletableFuture<SendResult<String, Object>> publishAsync(String topic, String key, Object event) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event asynchronously to topic={} with key={}", topic, key, ex);
            } else {
                log.info("Event published asynchronously to topic={} with key={}, offset={}",
                        topic, key, result.getRecordMetadata().offset());
            }
        });
        return future;
    }
}
