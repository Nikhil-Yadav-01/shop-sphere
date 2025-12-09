package com.rudraksha.shopsphere.analytics.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendEvent(String topic, Map<String, Object> event) {
        log.info("Sending event to topic {}: {}", topic, event);
        kafkaTemplate.send(topic, event);
    }
}
