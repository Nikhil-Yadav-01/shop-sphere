package com.rudraksha.shopsphere.coupon.controller;

import com.rudraksha.shopsphere.coupon.dto.request.*;
import com.rudraksha.shopsphere.coupon.dto.response.*;
import com.rudraksha.shopsphere.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.createCoupon(request));
    }

    @GetMapping("/{code}")
    public ResponseEntity<CouponResponse> getCoupon(@PathVariable String code) {
        return ResponseEntity.ok(couponService.getCoupon(code));
    }

    @GetMapping
    public ResponseEntity<List<CouponResponse>> getAllActiveCoupons() {
        return ResponseEntity.ok(couponService.getAllActiveCoupons());
    }

    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(@Valid @RequestBody ValidateCouponRequest request) {
        return ResponseEntity.ok(couponService.validateCoupon(request));
    }

    @PostMapping("/apply")
    public ResponseEntity<CouponValidationResponse> applyCoupon(@Valid @RequestBody ApplyCouponRequest request) {
        return ResponseEntity.ok(couponService.applyCoupon(request));
    }

    @PutMapping("/{code}/deactivate")
    public ResponseEntity<CouponResponse> deactivateCoupon(@PathVariable String code) {
        return ResponseEntity.ok(couponService.deactivateCoupon(code));
    }

    @PutMapping("/{code}")
    public ResponseEntity<CouponResponse> updateCoupon(
            @PathVariable String code,
            @Valid @RequestBody UpdateCouponRequest request) {
        return ResponseEntity.ok(couponService.updateCoupon(code, request));
    }

    @GetMapping("/all")
    public ResponseEntity<Page<CouponResponse>> getAllCoupons(Pageable pageable) {
        return ResponseEntity.ok(couponService.getAllCoupons(pageable));
    }

    @GetMapping("/users/{userId}/history")
    public ResponseEntity<List<CouponUsageResponse>> getUserCouponHistory(@PathVariable String userId) {
        return ResponseEntity.ok(couponService.getUserCouponHistory(userId));
    }

    @GetMapping("/{code}/usage-history")
    public ResponseEntity<List<CouponUsageResponse>> getCouponUsageHistory(@PathVariable String code) {
        return ResponseEntity.ok(couponService.getCouponUsageHistory(code));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable String code) {
        couponService.deleteCoupon(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/expired")
    public ResponseEntity<List<CouponResponse>> getExpiredCoupons() {
        return ResponseEntity.ok(couponService.getExpiredCoupons());
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<CouponResponse>> getCouponsExpiringIn(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(couponService.getCouponsExpiringIn(days));
    }
}