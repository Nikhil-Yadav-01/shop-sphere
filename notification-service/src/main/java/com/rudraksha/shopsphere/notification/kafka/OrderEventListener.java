package com.rudraksha.shopsphere.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rudraksha.shopsphere.notification.client.UserClient;
import com.rudraksha.shopsphere.notification.dto.NotificationRequest;
import com.rudraksha.shopsphere.notification.dto.OrderStateNotificationEvent;
import com.rudraksha.shopsphere.notification.dto.UserDetailsResponse;
import com.rudraksha.shopsphere.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventListener {

    private final NotificationService notificationService;
    private final UserClient userClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "notification.send", groupId = "notification-order-group")
    public void consumeOrderStateEvent(String message) {
        try {
            OrderStateNotificationEvent event = objectMapper.readValue(message, OrderStateNotificationEvent.class);
            log.info("Received order state event for order: {} with new status: {}", event.getOrderId(), event.getNewStatus());

            // Fetch user details to get email
            UserDetailsResponse user = userClient.getUserById(event.getUserId());
            
            String title = "Order Update: " + event.getNewStatus();
            String body = String.format("Hi %s, your order %s has been updated from %s to %s.", 
                    user.getFirstName(), event.getOrderId(), event.getPreviousStatus(), event.getNewStatus());

            // Send Email Notification
            NotificationRequest emailRequest = new NotificationRequest(
                    event.getUserId(),
                    "ORDER_UPDATE",
                    title,
                    body,
                    "EMAIL",
                    user.getEmail(),
                    null
            );
            notificationService.createNotification(emailRequest);

            // Send Push Notification
            NotificationRequest pushRequest = new NotificationRequest(
                    event.getUserId(),
                    "ORDER_UPDATE",
                    title,
                    body,
                    "PUSH",
                    null,
                    null
            );
            notificationService.createNotification(pushRequest);

            log.info("Processed order state notifications for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process order state event: {}", message, e);
        }
    }
}
