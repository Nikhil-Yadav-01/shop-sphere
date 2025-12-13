package com.rudraksha.shopsphere.returns.kafka;

import com.rudraksha.shopsphere.returns.entity.Return;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnEventProducer {

    private final KafkaTemplate<String, Return> kafkaTemplate;

    public void publishReturnEvent(Return returnEntity, String eventType) {
        try {
            String topic = "returns-events";
            kafkaTemplate.send(topic, returnEntity.getId(), returnEntity);
            log.info("Return event published: {} for return ID: {}", eventType, returnEntity.getId());
        } catch (Exception e) {
            log.error("Error publishing return event: {}", e.getMessage(), e);
        }
    }
}
