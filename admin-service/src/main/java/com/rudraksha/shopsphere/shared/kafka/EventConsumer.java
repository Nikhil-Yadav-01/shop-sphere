package com.rudraksha.shopsphere.shared.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Slf4j
public abstract class EventConsumer<T> {

    public void consume(ConsumerRecord<String, T> record) {
        String topic = record.topic();
        String key = record.key();
        T event = record.value();

        log.info("Received event from topic={} with key={}", topic, key);

        try {
            handleEvent(key, event);
            log.debug("Successfully processed event from topic={} with key={}", topic, key);
        } catch (Exception e) {
            log.error("Error processing event from topic={} with key={}", topic, key, e);
            handleError(key, event, e);
        }
    }

    protected abstract void handleEvent(String key, T event);

    protected void handleError(String key, T event, Exception exception) {
        log.error("Default error handler invoked for key={}, event={}", key, event, exception);
    }
}
