package com.rudraksha.shopsphere.fraud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rudraksha.shopsphere.fraud.dto.FraudCheckResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudEventPublisher {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String FRAUD_EVENTS_TOPIC = "fraud-detection-events";
    private static final String FRAUD_ALERT_TOPIC = "fraud-alerts";
    
    /**
     * Publish fraud detection event
     */
    public void publishFraudDetectionEvent(FraudCheckResponse fraudResponse) {
        try {
            String eventMessage = objectMapper.writeValueAsString(fraudResponse);
            
            Message<String> message = MessageBuilder
                    .withPayload(eventMessage)
                    .setHeader(KafkaHeaders.TOPIC, FRAUD_EVENTS_TOPIC)
                    .setHeader("kafka_messageKey", fraudResponse.getTransactionId())
                    .setHeader("event-type", "fraud-detection")
                    .setHeader("event-timestamp", System.currentTimeMillis())
                    .build();
            
            kafkaTemplate.send(message);
            log.info("Published fraud detection event for transaction: {}", fraudResponse.getTransactionId());
            
        } catch (Exception e) {
            log.error("Error publishing fraud detection event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Publish fraud alert for fraudulent transactions
     */
    public void publishFraudAlert(FraudCheckResponse fraudResponse) {
        if (fraudResponse.getIsFraudulent()) {
            try {
                String alertMessage = objectMapper.writeValueAsString(fraudResponse);
                
                Message<String> message = MessageBuilder
                        .withPayload(alertMessage)
                        .setHeader(KafkaHeaders.TOPIC, FRAUD_ALERT_TOPIC)
                        .setHeader("kafka_messageKey", fraudResponse.getTransactionId())
                        .setHeader("event-type", "fraud-alert")
                        .setHeader("severity", "HIGH")
                        .setHeader("event-timestamp", System.currentTimeMillis())
                        .build();
                
                kafkaTemplate.send(message);
                log.warn("Published FRAUD ALERT for transaction: {} - Customer: {}", 
                        fraudResponse.getTransactionId(), fraudResponse.getCustomerId());
                
            } catch (Exception e) {
                log.error("Error publishing fraud alert: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * Publish transaction approved event
     */
    public void publishTransactionApprovedEvent(FraudCheckResponse fraudResponse) {
        if (!fraudResponse.getIsFraudulent()) {
            try {
                String eventMessage = objectMapper.writeValueAsString(fraudResponse);
                kafkaTemplate.send(FRAUD_EVENTS_TOPIC, fraudResponse.getTransactionId(), eventMessage);
                log.info("Published transaction approved event for: {}", fraudResponse.getTransactionId());
            } catch (Exception e) {
                log.error("Error publishing approved event: {}", e.getMessage(), e);
            }
        }
    }
}
