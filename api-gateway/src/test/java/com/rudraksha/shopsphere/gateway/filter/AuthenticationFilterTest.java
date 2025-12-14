package com.rudraksha.shopsphere.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchangeBuilder;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
        "auth.service.url=http://localhost:8081",
        "auth.request.timeout=5000"
})
class AuthenticationFilterTest {

    private AuthenticationFilter authenticationFilter;
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        authenticationFilter = new AuthenticationFilter(null, redisTemplate);
    }

    @Test
    void testMissingAuthenticationHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = new DefaultServerWebExchangeBuilder(request).build();

        var filter = authenticationFilter.apply(new Object());
        var result = filter.filter(exchange, chain -> null);

        assert exchange.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED;
    }

    @Test
    void testInvalidBearerToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header(HttpHeaders.AUTHORIZATION, "InvalidToken")
                .build();
        ServerWebExchange exchange = new DefaultServerWebExchangeBuilder(request).build();

        var filter = authenticationFilter.apply(new Object());
        var result = filter.filter(exchange, chain -> null);

        assert exchange.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED;
    }
}
