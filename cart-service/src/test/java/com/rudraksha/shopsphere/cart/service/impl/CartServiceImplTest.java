package com.rudraksha.shopsphere.cart.service.impl;

import com.rudraksha.shopsphere.cart.client.CatalogClient;
import com.rudraksha.shopsphere.cart.client.InventoryClient;
import com.rudraksha.shopsphere.cart.dto.request.AddToCartRequest;
import com.rudraksha.shopsphere.cart.dto.request.UpdateCartItemRequest;
import com.rudraksha.shopsphere.cart.dto.response.CartResponse;
import com.rudraksha.shopsphere.cart.dto.response.ProductResponse;
import com.rudraksha.shopsphere.cart.entity.Cart;
import com.rudraksha.shopsphere.cart.entity.CartItem;
import com.rudraksha.shopsphere.cart.exception.InsufficientStockException;
import com.rudraksha.shopsphere.cart.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CatalogClient catalogClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private CartServiceImpl cartService;

    private String userId = "user-123";
    private String productId = "prod-123";
    private String sku = "SKU-123";
    private ProductResponse productResponse;
    private Cart cart;

    @BeforeEach
    void setUp() {
        productResponse = ProductResponse.builder()
                .id(productId)
                .sku(sku)
                .name("Test Product")
                .price(BigDecimal.valueOf(100.00))
                .build();

        cart = Cart.builder()
                .userId(userId)
                .items(new ArrayList<>())
                .build();
    }

    @Test
    void getCart_Success() {
        when(cartRepository.findById(userId)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCart(userId);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        verify(cartRepository).findById(userId);
    }

    @Test
    void addToCart_Success() {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(productId);
        request.setQuantity(2);

        when(catalogClient.getProductById(productId)).thenReturn(productResponse);
        when(inventoryClient.checkAvailability(sku, 2)).thenReturn(true);
        when(cartRepository.findById(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = cartService.addToCart(userId, request);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addToCart_InsufficientStock() {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(productId);
        request.setQuantity(10);

        when(catalogClient.getProductById(productId)).thenReturn(productResponse);
        when(inventoryClient.checkAvailability(sku, 10)).thenReturn(false);

        assertThrows(InsufficientStockException.class, () -> cartService.addToCart(userId, request));
    }

    @Test
    void updateCartItem_Success() {
        CartItem item = CartItem.builder().productId(productId).quantity(1).price(BigDecimal.TEN).build();
        cart.getItems().add(item);
        
        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(5);

        when(cartRepository.findById(userId)).thenReturn(Optional.of(cart));
        when(catalogClient.getProductById(productId)).thenReturn(productResponse);
        when(inventoryClient.checkAvailability(sku, 5)).thenReturn(true);

        CartResponse response = cartService.updateCartItem(userId, productId, request);

        assertNotNull(response);
        assertEquals(5, response.getItems().get(0).getQuantity());
    }

    @Test
    void removeFromCart_Success() {
        CartItem item = CartItem.builder().productId(productId).quantity(1).price(BigDecimal.TEN).build();
        cart.getItems().add(item);

        when(cartRepository.findById(userId)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.removeFromCart(userId, productId);

        assertTrue(response.getItems().isEmpty());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void clearCart_Success() {
        when(cartRepository.findById(userId)).thenReturn(Optional.of(cart));
        doNothing().when(cartRepository).delete(cart);

        assertDoesNotThrow(() -> cartService.clearCart(userId));
        verify(cartRepository).delete(cart);
    }
}
