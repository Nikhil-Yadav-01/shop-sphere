package com.rudraksha.shopsphere.coupon.service.impl;

import com.rudraksha.shopsphere.coupon.dto.request.*;
import com.rudraksha.shopsphere.coupon.dto.response.*;
import com.rudraksha.shopsphere.coupon.entity.*;
import com.rudraksha.shopsphere.coupon.repository.*;
import com.rudraksha.shopsphere.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rudraksha.shopsphere.coupon.exception.CouponException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CouponServiceImpl implements CouponService {
    
    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    
    @Override
    public CouponResponse createCoupon(CreateCouponRequest request) {
        if (couponRepository.findByCodeAndActiveTrue(request.getCode()).isPresent()) {
            throw new RuntimeException("Coupon with code " + request.getCode() + " already exists");
        }
        
        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minimumOrderAmount(request.getMinimumOrderAmount())
                .maximumDiscountAmount(request.getMaximumDiscountAmount())
                .usageLimit(request.getUsageLimit())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .build();
        
        coupon = couponRepository.save(coupon);
        log.info("Created coupon with code: {}", coupon.getCode());
        
        return mapToResponse(coupon);
    }
    
    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCoupon(String code) {
        Coupon coupon = couponRepository.findByCodeAndActiveTrue(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Coupon not found: " + code));
        return mapToResponse(coupon);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getAllActiveCoupons() {
        return couponRepository.findActiveCoupons(LocalDateTime.now())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(ValidateCouponRequest request) {
        String code = request.getCouponCode().toUpperCase();
        
        Coupon coupon = couponRepository.findValidCoupon(code, LocalDateTime.now())
                .orElse(null);
        
        if (coupon == null) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .message("Invalid or expired coupon")
                    .couponCode(code)
                    .build();
        }
        
        if (coupon.getMinimumOrderAmount() != null && 
            request.getOrderAmount().compareTo(coupon.getMinimumOrderAmount()) < 0) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .message("Minimum order amount not met. Required: " + coupon.getMinimumOrderAmount())
                    .couponCode(code)
                    .build();
        }
        
        BigDecimal discountAmount = calculateDiscount(coupon, request.getOrderAmount());
        BigDecimal finalAmount = request.getOrderAmount().subtract(discountAmount);
        
        return CouponValidationResponse.builder()
                .valid(true)
                .message("Coupon is valid")
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .couponCode(code)
                .build();
    }
    
    @Override
    public CouponValidationResponse applyCoupon(ApplyCouponRequest request) {
        String code = request.getCouponCode().toUpperCase();
        
        Coupon coupon = couponRepository.findValidCoupon(code, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired coupon"));
        
        if (coupon.getMinimumOrderAmount() != null && 
            request.getOrderAmount().compareTo(coupon.getMinimumOrderAmount()) < 0) {
            throw new RuntimeException("Minimum order amount not met");
        }
        
        BigDecimal discountAmount = calculateDiscount(coupon, request.getOrderAmount());
        
        CouponUsage usage = CouponUsage.builder()
                .couponCode(code)
                .userId(request.getUserId())
                .orderId(request.getOrderId())
                .discountAmount(discountAmount)
                .build();
        
        couponUsageRepository.save(usage);
        
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);
        
        BigDecimal finalAmount = request.getOrderAmount().subtract(discountAmount);
        
        log.info("Applied coupon {} for user {} on order {}, discount: {}", 
                code, request.getUserId(), request.getOrderId(), discountAmount);
        
        return CouponValidationResponse.builder()
                .valid(true)
                .message("Coupon applied successfully")
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .couponCode(code)
                .build();
    }
    
    @Override
    public CouponResponse deactivateCoupon(String code) {
        Coupon coupon = couponRepository.findByCodeAndActiveTrue(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Coupon not found: " + code));
        
        coupon.setActive(false);
        coupon = couponRepository.save(coupon);
        
        log.info("Deactivated coupon: {}", code);
        return mapToResponse(coupon);
    }
    
    @Override
    public CouponResponse updateCoupon(String code, UpdateCouponRequest request) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Coupon not found: " + code));
        
        if (request.getDescription() != null) coupon.setDescription(request.getDescription());
        if (request.getDiscountValue() != null) coupon.setDiscountValue(request.getDiscountValue());
        if (request.getMinimumOrderAmount() != null) coupon.setMinimumOrderAmount(request.getMinimumOrderAmount());
        if (request.getMaximumDiscountAmount() != null) coupon.setMaximumDiscountAmount(request.getMaximumDiscountAmount());
        if (request.getUsageLimit() != null) coupon.setUsageLimit(request.getUsageLimit());
        if (request.getValidFrom() != null) coupon.setValidFrom(request.getValidFrom());
        if (request.getValidUntil() != null) coupon.setValidUntil(request.getValidUntil());
        if (request.getActive() != null) coupon.setActive(request.getActive());
        
        coupon = couponRepository.save(coupon);
        log.info("Updated coupon: {}", code);
        return mapToResponse(coupon);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<CouponResponse> getAllCoupons(Pageable pageable) {
        return couponRepository.findAll(pageable)
                .map(this::mapToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CouponUsageResponse> getUserCouponHistory(String userId) {
        return couponUsageRepository.findByUserId(userId)
                .stream()
                .map(this::mapToUsageResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CouponUsageResponse> getCouponUsageHistory(String couponCode) {
        return couponUsageRepository.findByCouponCode(couponCode.toUpperCase())
                .stream()
                .map(this::mapToUsageResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteCoupon(String code) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Coupon not found: " + code));
        couponRepository.delete(coupon);
        log.info("Deleted coupon: {}", code);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getExpiredCoupons() {
        return couponRepository.findExpiredCoupons(LocalDateTime.now())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getCouponsExpiringIn(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusDays(days);
        return couponRepository.findCouponsExpiringBetween(now, futureDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount) {
        BigDecimal discount;
        
        if (coupon.getDiscountType() == Coupon.DiscountType.PERCENTAGE) {
            discount = orderAmount.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = coupon.getDiscountValue();
        }
        
        if (coupon.getMaximumDiscountAmount() != null && 
            discount.compareTo(coupon.getMaximumDiscountAmount()) > 0) {
            discount = coupon.getMaximumDiscountAmount();
        }
        
        return discount.min(orderAmount);
    }
    
    private CouponResponse mapToResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minimumOrderAmount(coupon.getMinimumOrderAmount())
                .maximumDiscountAmount(coupon.getMaximumDiscountAmount())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .active(coupon.getActive())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }
    
    private CouponUsageResponse mapToUsageResponse(CouponUsage usage) {
        return CouponUsageResponse.builder()
                .id(usage.getId())
                .couponCode(usage.getCouponCode())
                .userId(usage.getUserId())
                .orderId(usage.getOrderId())
                .discountAmount(usage.getDiscountAmount())
                .usedAt(usage.getUsedAt())
                .build();
    }
}