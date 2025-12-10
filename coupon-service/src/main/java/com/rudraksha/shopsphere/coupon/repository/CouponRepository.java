package com.rudraksha.shopsphere.coupon.repository;

import com.rudraksha.shopsphere.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeAndActiveTrue(String code);
    Optional<Coupon> findByCode(String code);
    
    @Query("SELECT c FROM Coupon c WHERE c.active = true AND c.validFrom <= :now AND c.validUntil >= :now")
    List<Coupon> findActiveCoupons(@Param("now") LocalDateTime now);
    
    @Query("SELECT c FROM Coupon c WHERE c.code = :code AND c.active = true AND c.validFrom <= :now AND c.validUntil >= :now AND c.usedCount < c.usageLimit")
    Optional<Coupon> findValidCoupon(@Param("code") String code, @Param("now") LocalDateTime now);
    
    @Query("SELECT c FROM Coupon c WHERE c.active = true AND c.validUntil < :now")
    List<Coupon> findExpiredCoupons(@Param("now") LocalDateTime now);
    
    @Query("SELECT c FROM Coupon c WHERE c.active = true AND c.validUntil BETWEEN :now AND :futureDate")
    List<Coupon> findCouponsExpiringBetween(@Param("now") LocalDateTime now, @Param("futureDate") LocalDateTime futureDate);
}