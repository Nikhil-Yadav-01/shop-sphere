package com.rudraksha.shopsphere.coupon.service;

import com.rudraksha.shopsphere.coupon.dto.request.*;
import com.rudraksha.shopsphere.coupon.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CouponService {
    CouponResponse createCoupon(CreateCouponRequest request);
    CouponResponse getCoupon(String code);
    CouponResponse updateCoupon(String code, UpdateCouponRequest request);
    List<CouponResponse> getAllActiveCoupons();
    Page<CouponResponse> getAllCoupons(Pageable pageable);
    CouponValidationResponse validateCoupon(ValidateCouponRequest request);
    CouponValidationResponse applyCoupon(ApplyCouponRequest request);
    CouponResponse deactivateCoupon(String code);
    List<CouponUsageResponse> getUserCouponHistory(String userId);
    List<CouponUsageResponse> getCouponUsageHistory(String couponCode);
    void deleteCoupon(String code);
    List<CouponResponse> getExpiredCoupons();
    List<CouponResponse> getCouponsExpiringIn(int days);
}