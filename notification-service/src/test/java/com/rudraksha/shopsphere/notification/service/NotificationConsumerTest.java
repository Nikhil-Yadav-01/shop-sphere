package com.rudraksha.shopsphere.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rudraksha.shopsphere.notification.dto.EmailNotificationEvent;
import com.rudraksha.shopsphere.notification.dto.NotificationResultEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationConsumerTest {

    @Mock
    private EmailService emailService;
    
    @Mock
    private PushNotificationService pushNotificationService;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    private EmailNotificationEvent event;
    private String jsonMessage;

    @BeforeEach
    void setUp() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "John");
        
        event = EmailNotificationEvent.builder()
                .to("test@example.com")
                .subject("Welcome")
                .templateName("welcome-email")
                .templateVariables(variables)
                .replyToTopic("auth-feedback")
                .correlationId("corr-123")
                .build();
        
        jsonMessage = "{\"to\":\"test@example.com\",\"subject\":\"Welcome\",\"templateName\":\"welcome-email\"}";
    }

    @Test
    void testConsumeEmailNotificationEvent_WithTemplate() throws Exception {
        when(objectMapper.readValue(anyString(), eq(EmailNotificationEvent.class))).thenReturn(event);

        notificationConsumer.consumeEmailNotificationEvent(jsonMessage);

        verify(emailService).sendHtmlEmail(eq("test@example.com"), eq("Welcome"), eq("welcome-email"), anyMap());
        
        ArgumentCaptor<NotificationResultEvent> captor = ArgumentCaptor.forClass(NotificationResultEvent.class);
        verify(kafkaTemplate).send(eq("auth-feedback"), eq("corr-123"), captor.capture());
        
        NotificationResultEvent result = captor.getValue();
        assertEquals("corr-123", result.getCorrelationId());
        assertEquals("SUCCESS", result.getStatus());
    }

    @Test
    void testConsumeEmailNotificationEvent_FallbackToGeneric() throws Exception {
        event.setTemplateName(null);
        event.setBody("Hello World");
        when(objectMapper.readValue(anyString(), eq(EmailNotificationEvent.class))).thenReturn(event);

        notificationConsumer.consumeEmailNotificationEvent(jsonMessage);

        verify(emailService).sendHtmlEmail(eq("test@example.com"), eq("Welcome"), eq("email-template"), anyMap());
        verify(kafkaTemplate).send(eq("auth-feedback"), eq("corr-123"), any());
    }

    @Test
    void testConsumeEmailNotificationEvent_FailureSendsFeedback() throws Exception {
        when(objectMapper.readValue(anyString(), eq(EmailNotificationEvent.class))).thenReturn(event);
        doThrow(new RuntimeException("Mail server down")).when(emailService).sendHtmlEmail(any(), any(), any(), any());

        try {
            notificationConsumer.consumeEmailNotificationEvent(jsonMessage);
        } catch (RuntimeException e) {
            // expected
        }

        ArgumentCaptor<NotificationResultEvent> captor = ArgumentCaptor.forClass(NotificationResultEvent.class);
        verify(kafkaTemplate).send(eq("auth-feedback"), eq("corr-123"), captor.capture());
        
        NotificationResultEvent result = captor.getValue();
        assertEquals("FAILED", result.getStatus());
        assertEquals("Mail server down", result.getErrorReason());
    }
}
