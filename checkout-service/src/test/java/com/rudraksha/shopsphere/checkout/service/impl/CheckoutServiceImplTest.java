package com.rudraksha.shopsphere.checkout.service.impl;

import com.rudraksha.shopsphere.checkout.dto.request.CheckoutRequest;
import com.rudraksha.shopsphere.checkout.dto.request.ShippingAddressRequest;
import com.rudraksha.shopsphere.checkout.dto.response.OrderResponse;
import com.rudraksha.shopsphere.checkout.service.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceImplTest {

    @Mock
    private CheckoutServiceImpl.CartClient cartClient;

    @Mock
    private CheckoutServiceImpl.OrderClient orderClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private CheckoutServiceImpl checkoutService;

    private String userId = "user-123";
    private CheckoutServiceImpl.CartClient.CartResponse cartResponse;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        cartResponse = new CheckoutServiceImpl.CartClient.CartResponse(
                "cart-1", userId, 
                Collections.singletonList(new CheckoutServiceImpl.CartClient.CartItemResponse("p1", "Product 1", 1, BigDecimal.valueOf(100.00), BigDecimal.valueOf(100.00))),
                1, BigDecimal.valueOf(100.00)
        );

        orderResponse = OrderResponse.builder()
                .id(1L)
                .orderNumber("ORD-123")
                .status("PENDING")
                .build();
    }

    @Test
    void processCheckout_Success() {
        CheckoutRequest request = new CheckoutRequest();
        ShippingAddressRequest address = new ShippingAddressRequest();
        address.setAddressLine1("123 Street");
        address.setCity("City");
        request.setShippingAddress(address);

        when(cartClient.getCart(userId)).thenReturn(cartResponse);
        when(orderClient.createOrder(any())).thenReturn(orderResponse);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());

        OrderResponse response = checkoutService.processCheckout(userId, request);

        assertNotNull(response);
        assertEquals("ORD-123", response.getOrderNumber());
        verify(cartClient).getCart(userId);
        verify(orderClient).createOrder(any());
        verify(kafkaTemplate).send(eq("checkout.initiated"), eq("ORD-123"), any());
    }

    @Test
    void processCheckout_EmptyCart() {
        when(cartClient.getCart(userId)).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> checkoutService.processCheckout(userId, new CheckoutRequest()));
    }
}
