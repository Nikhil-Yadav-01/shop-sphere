package com.rudraksha.shopsphere.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.handler.ResponseStatusExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GlobalErrorHandler extends ResponseStatusExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalErrorHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        logger.error("Global error handler triggered", ex);

        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

        if (ex.getCause() instanceof IllegalArgumentException) {
            httpStatus = HttpStatus.BAD_REQUEST;
        } else if (ex.getMessage() != null && ex.getMessage().contains("401")) {
            httpStatus = HttpStatus.UNAUTHORIZED;
        } else if (ex.getMessage() != null && ex.getMessage().contains("403")) {
            httpStatus = HttpStatus.FORBIDDEN;
        } else if (ex.getMessage() != null && ex.getMessage().contains("404")) {
            httpStatus = HttpStatus.NOT_FOUND;
        }

        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", httpStatus.value());
        errorResponse.put("error", httpStatus.getReasonPhrase());
        errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : "An error occurred");
        errorResponse.put("path", exchange.getRequest().getPath().toString());
        
        // Add tracing headers for request tracking
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-ID");
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-ID");
        String requestId = exchange.getRequest().getId();
        
        if (correlationId != null) {
            errorResponse.put("correlationId", correlationId);
        }
        if (traceId != null) {
            errorResponse.put("traceId", traceId);
        }
        if (requestId != null) {
            errorResponse.put("requestId", requestId);
        }
        
        logger.error("Error Response - Status: {}, CorrelationId: {}, Message: {}", 
                    httpStatus.value(), correlationId, ex.getMessage());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(reactor.core.publisher.Mono.just(buffer));
        } catch (JsonProcessingException e) {
            logger.error("Error serializing error response", e);
            return exchange.getResponse().setComplete();
        }
    }
}
