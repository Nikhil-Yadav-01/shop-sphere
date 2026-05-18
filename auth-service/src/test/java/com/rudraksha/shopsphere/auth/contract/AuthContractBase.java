package com.rudraksha.shopsphere.auth.contract;

import com.rudraksha.shopsphere.auth.controller.AuthController;
import com.rudraksha.shopsphere.auth.dto.response.AuthResponse;
import com.rudraksha.shopsphere.auth.dto.response.TokenValidationResponse;
import com.rudraksha.shopsphere.auth.service.AuthService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AuthController.class, properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
public abstract class AuthContractBase {

    @MockBean
    private AuthService authService;

    @BeforeEach
    public void setup() {
        // Mocking /auth/login response
        AuthResponse mockLoginResponse = AuthResponse.builder()
                .accessToken("mocked_access_jwt_token")
                .refreshToken("mocked_refresh_jwt_token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .email("user@example.com")
                .firstName("John")
                .lastName("Doe")
                .role("CUSTOMER")
                .build();
        when(authService.login(any())).thenReturn(mockLoginResponse);

        // Mocking /auth/validate response
        TokenValidationResponse mockValidationResponse = new TokenValidationResponse(
                true,
                "d3b07384-d113-4a1d-a5ee-a83d0382bc52",
                Collections.singletonList("CUSTOMER")
        );
        when(authService.validateToken("valid_jwt_token")).thenReturn(mockValidationResponse);

        // Setup Standalone MockMvc with the controller and mocked service
        AuthController authController = new AuthController(authService);
        RestAssuredMockMvc.standaloneSetup(authController);
    }
}
