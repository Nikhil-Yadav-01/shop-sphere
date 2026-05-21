package com.rudraksha.shopsphere.pricing.service.impl;

import com.rudraksha.shopsphere.pricing.dto.request.CalculatePriceRequest;
import com.rudraksha.shopsphere.pricing.dto.request.CreateProductPriceRequest;
import com.rudraksha.shopsphere.pricing.dto.response.PriceCalculationResponse;
import com.rudraksha.shopsphere.pricing.dto.response.ProductPriceResponse;
import com.rudraksha.shopsphere.pricing.entity.ProductPrice;
import com.rudraksha.shopsphere.pricing.exception.PricingException;
import com.rudraksha.shopsphere.pricing.repository.PriceHistoryRepository;
import com.rudraksha.shopsphere.pricing.repository.PricingRuleRepository;
import com.rudraksha.shopsphere.pricing.repository.PricingTierRepository;
import com.rudraksha.shopsphere.pricing.repository.ProductPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingServiceImplTest {

    @Mock
    private ProductPriceRepository productPriceRepository;
    @Mock
    private PricingRuleRepository pricingRuleRepository;
    @Mock
    private PricingTierRepository pricingTierRepository;
    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @InjectMocks
    private PricingServiceImpl pricingService;

    private ProductPrice productPrice;
    private String productId = "PROD-123";

    @BeforeEach
    void setUp() {
        productPrice = ProductPrice.builder()
                .id(1L)
                .productId(productId)
                .basePrice(BigDecimal.valueOf(100.00))
                .currency("USD")
                .active(true)
                .build();
    }

    @Test
    void createProductPrice_Success() {
        CreateProductPriceRequest request = new CreateProductPriceRequest();
        request.setProductId(productId);
        request.setBasePrice(BigDecimal.valueOf(100.00));
        request.setCurrency("USD");

        when(productPriceRepository.findByProductId(productId)).thenReturn(Optional.empty());
        when(productPriceRepository.save(any(ProductPrice.class))).thenReturn(productPrice);

        ProductPriceResponse response = pricingService.createProductPrice(request);

        assertNotNull(response);
        assertEquals(productId, response.getProductId());
        verify(productPriceRepository).save(any(ProductPrice.class));
    }

    @Test
    void createProductPrice_AlreadyExists() {
        CreateProductPriceRequest request = new CreateProductPriceRequest();
        request.setProductId(productId);

        when(productPriceRepository.findByProductId(productId)).thenReturn(Optional.of(productPrice));

        assertThrows(PricingException.class, () -> pricingService.createProductPrice(request));
    }

    @Test
    void getProductPrice_Success() {
        when(productPriceRepository.findByProductIdAndActiveTrue(productId)).thenReturn(Optional.of(productPrice));

        ProductPriceResponse response = pricingService.getProductPrice(productId);

        assertNotNull(response);
        assertEquals(productId, response.getProductId());
    }

    @Test
    void calculatePrice_NoDiscounts() {
        CalculatePriceRequest request = new CalculatePriceRequest();
        request.setProductId(productId);
        request.setQuantity(1);

        when(productPriceRepository.findByProductIdAndActiveTrue(productId)).thenReturn(Optional.of(productPrice));
        when(pricingTierRepository.findTierByQuantity(1)).thenReturn(Optional.empty());
        when(pricingRuleRepository.findAllActiveRules(any())).thenReturn(Collections.emptyList());

        PriceCalculationResponse response = pricingService.calculatePrice(request);

        assertNotNull(response);
        assertTrue(BigDecimal.valueOf(100.0).compareTo(response.getUnitPrice()) == 0);
        assertTrue(BigDecimal.valueOf(100.0).compareTo(response.getTotalPrice()) == 0);
    }
}
