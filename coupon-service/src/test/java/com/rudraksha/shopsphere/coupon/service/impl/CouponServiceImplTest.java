package com.rudraksha.shopsphere.coupon.service.impl;

import com.rudraksha.shopsphere.coupon.dto.request.ApplyCouponRequest;
import com.rudraksha.shopsphere.coupon.dto.request.CreateCouponRequest;
import com.rudraksha.shopsphere.coupon.dto.request.ValidateCouponRequest;
import com.rudraksha.shopsphere.coupon.dto.response.CouponResponse;
import com.rudraksha.shopsphere.coupon.dto.response.CouponValidationResponse;
import com.rudraksha.shopsphere.coupon.entity.Coupon;
import com.rudraksha.shopsphere.coupon.repository.CouponRepository;
import com.rudraksha.shopsphere.coupon.repository.CouponUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private CouponUsageRepository couponUsageRepository;

    @InjectMocks
    private CouponServiceImpl couponService;

    private Coupon coupon;
    private String couponCode = "SAVE10";

    @BeforeEach
    void setUp() {
        coupon = Coupon.builder()
                .id(1L)
                .code(couponCode)
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .minimumOrderAmount(BigDecimal.valueOf(50))
                .active(true)
                .usedCount(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(1))
                .build();
    }

    @Test
    void createCoupon_Success() {
        CreateCouponRequest request = new CreateCouponRequest();
        request.setCode(couponCode);
        request.setDiscountType(Coupon.DiscountType.PERCENTAGE);
        request.setDiscountValue(BigDecimal.valueOf(10));

        when(couponRepository.findByCodeAndActiveTrue(couponCode)).thenReturn(Optional.empty());
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

        CouponResponse response = couponService.createCoupon(request);

        assertNotNull(response);
        assertEquals(couponCode, response.getCode());
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    void validateCoupon_Valid() {
        ValidateCouponRequest request = new ValidateCouponRequest();
        request.setCouponCode(couponCode);
        request.setOrderAmount(BigDecimal.valueOf(100));

        when(couponRepository.findValidCoupon(eq(couponCode), any())).thenReturn(Optional.of(coupon));

        CouponValidationResponse response = couponService.validateCoupon(request);

        assertTrue(response.getValid());
        assertEquals(BigDecimal.valueOf(10.00).setScale(2), response.getDiscountAmount());
    }

    @Test
    void applyCoupon_Success() {
        ApplyCouponRequest request = new ApplyCouponRequest();
        request.setCouponCode(couponCode);
        request.setUserId("user-1");
        request.setOrderId("order-1");
        request.setOrderAmount(BigDecimal.valueOf(100));

        when(couponRepository.findValidCoupon(eq(couponCode), any())).thenReturn(Optional.of(coupon));

        CouponValidationResponse response = couponService.applyCoupon(request);

        assertTrue(response.getValid());
        verify(couponUsageRepository).save(any());
        verify(couponRepository).save(coupon);
    }
}
