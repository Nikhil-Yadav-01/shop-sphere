package com.rudraksha.shopsphere.pricing.repository;

import com.rudraksha.shopsphere.pricing.entity.ProductPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductPriceRepository extends JpaRepository<ProductPrice, Long> {
    Optional<ProductPrice> findByProductId(String productId);
    Optional<ProductPrice> findByProductIdAndActiveTrue(String productId);
    List<ProductPrice> findAllByActiveTrue();
    
    @Query("SELECT p FROM ProductPrice p WHERE p.active = true ORDER BY p.productId")
    List<ProductPrice> findActiveProducts();
}
