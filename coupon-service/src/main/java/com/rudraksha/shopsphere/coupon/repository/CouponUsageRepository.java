package com.rudraksha.shopsphere.coupon.repository;

import com.rudraksha.shopsphere.coupon.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    List<CouponUsage> findByUserId(String userId);
    List<CouponUsage> findByCouponCode(String couponCode);
    boolean existsByCouponCodeAndUserId(String couponCode, String userId);
}