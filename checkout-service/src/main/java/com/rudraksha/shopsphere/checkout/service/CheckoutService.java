package com.rudraksha.shopsphere.checkout.service;

import com.rudraksha.shopsphere.checkout.dto.request.CheckoutRequest;
import com.rudraksha.shopsphere.checkout.dto.response.CheckoutSessionResponse;
import com.rudraksha.shopsphere.checkout.event.InventoryReservationFailedPayload;
import com.rudraksha.shopsphere.checkout.event.InventoryReservedPayload;
import com.rudraksha.shopsphere.checkout.event.PaymentFailedPayload;
import com.rudraksha.shopsphere.checkout.event.PaymentSucceededPayload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CheckoutService {

    /**
     * Initiate a checkout session. Returns 202 Accepted immediately.
     * The actual order/payment flow is async via SAGA.
     */
    CheckoutSessionResponse initiateCheckout(String userId, CheckoutRequest request, String idempotencyKey);

    /**
     * Poll the status of a checkout session.
     */
    CheckoutSessionResponse getCheckoutSession(String sessionId);

    /**
     * Get all checkout sessions for a user, paginated.
     */
    Page<CheckoutSessionResponse> getUserSessions(String userId, Pageable pageable);

    // ── SAGA Callbacks (called from Kafka listeners) ─────────────────────────

    void handleInventoryReserved(InventoryReservedPayload payload);

    void handleInventoryReservationFailed(InventoryReservationFailedPayload payload);

    void handlePaymentSucceeded(PaymentSucceededPayload payload);

    void handlePaymentFailed(PaymentFailedPayload payload);
}