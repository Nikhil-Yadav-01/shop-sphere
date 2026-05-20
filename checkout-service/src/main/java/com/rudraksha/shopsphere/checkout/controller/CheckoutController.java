package com.rudraksha.shopsphere.checkout.controller;

import com.rudraksha.shopsphere.checkout.dto.request.CheckoutRequest;
import com.rudraksha.shopsphere.checkout.dto.response.CheckoutSessionResponse;
import com.rudraksha.shopsphere.checkout.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {

    private final CheckoutService checkoutService;

    /**
     * Initiate a checkout session asynchronously.
     * Returns 202 Accepted immediately; client should poll GET /sessions/{sessionId} for status.
     *
     * @param userId         extracted from gateway-injected X-User-Id header
     * @param idempotencyKey optional client-supplied idempotency key (Idempotency-Key header)
     * @param request        checkout request body with shipping address and payment details
     */
    @PostMapping("/sessions")
    public ResponseEntity<CheckoutSessionResponse> initiateCheckout(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request) {
        log.info("POST /api/v1/checkout/sessions userId={}", userId);
        CheckoutSessionResponse response = checkoutService.initiateCheckout(userId, request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Poll the status of a checkout session.
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<CheckoutSessionResponse> getSession(@PathVariable String sessionId) {
        log.debug("GET /api/v1/checkout/sessions/{}", sessionId);
        return ResponseEntity.ok(checkoutService.getCheckoutSession(sessionId));
    }

    /**
     * Get all checkout sessions for a user, paginated.
     */
    @GetMapping("/sessions")
    public ResponseEntity<Page<CheckoutSessionResponse>> getUserSessions(
            @RequestHeader("X-User-Id") String userId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("GET /api/v1/checkout/sessions userId={}", userId);
        return ResponseEntity.ok(checkoutService.getUserSessions(userId, pageable));
    }
}