package com.rudraksha.shopsphere.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("local")
public class LocalConfig {

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        // Mock Redis connection for local testing
        return mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate() {
        // Mock Kafka template for local testing
        return mock(KafkaTemplate.class);
    }

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        // Mock mail sender for local testing
        return mock(JavaMailSenderImpl.class);
    }
}