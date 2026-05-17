package com.rudraksha.shopsphere.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rudraksha.shopsphere.auth.dto.request.*;
import com.rudraksha.shopsphere.auth.entity.User;
import com.rudraksha.shopsphere.auth.repository.UserRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
public class AuthProductionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private KafkaConsumer<String, String> kafkaConsumer;

    @BeforeEach
    void setUpConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "auth-production-test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        kafkaConsumer = new KafkaConsumer<>(props);
        kafkaConsumer.subscribe(Collections.singletonList("user.created"));
        
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDownConsumer() {
        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }
    }

    @Test
    public void testCompleteRealServiceLifecycleFlow() throws Exception {
        // Step 1: Register User (POST /auth/register)
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("real-integration-user@example.com")
                .password("SecurePassword123!")
                .firstName("Real")
                .lastName("Integration")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("real-integration-user@example.com"));

        // Assert database persistence (real Postgres query)
        User persistedUser = userRepository.findByEmail("real-integration-user@example.com")
                .orElseThrow(() -> new AssertionError("User should be persisted in real PostgreSQL"));

        assertFalse(persistedUser.isEmailVerified());
        assertNotNull(persistedUser.getVerificationToken());
        String verificationToken = persistedUser.getVerificationToken();

        // Assert event was published to Kafka in real time
        boolean messageReceived = false;
        long endTime = System.currentTimeMillis() + 10000; // 10 seconds timeout
        while (System.currentTimeMillis() < endTime) {
            ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(200));
            for (ConsumerRecord<String, String> record : records) {
                if (record.value().contains("real-integration-user@example.com")) {
                    messageReceived = true;
                    break;
                }
            }
            if (messageReceived) break;
        }

        assertTrue(messageReceived, "Verification event should be successfully consumed from real Kafka broker");

        // Step 2: Verify Email (POST /auth/verify-email)
        VerifyEmailRequest verifyRequest = new VerifyEmailRequest();
        verifyRequest.setToken(verificationToken);

        mockMvc.perform(post("/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk());

        // Assert database state update (isEmailVerified = true)
        User verifiedUser = userRepository.findByEmail("real-integration-user@example.com").get();
        assertTrue(verifiedUser.isEmailVerified());

        // Step 3: Login User (POST /auth/login)
        LoginRequest loginRequest = LoginRequest.builder()
                .email("real-integration-user@example.com")
                .password("SecurePassword123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody).get("accessToken").asText();
        String refreshToken = objectMapper.readTree(responseBody).get("refreshToken").asText();

        // Step 4: Refresh Token (POST /auth/refresh)
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);

        MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        String newRefreshToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).get("refreshToken").asText();
        assertNotEquals(refreshToken, newRefreshToken); // Verify token rotation worked!

        // Step 5: Logout (POST /auth/logout)
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        // Assert token is blacklisted in real Redis cache
        Boolean isBlacklisted = redisTemplate.hasKey("revoked_token:" + accessToken);
        assertTrue(isBlacklisted, "Token should be correctly blacklisted in real Redis store upon user logout");
    }
}
