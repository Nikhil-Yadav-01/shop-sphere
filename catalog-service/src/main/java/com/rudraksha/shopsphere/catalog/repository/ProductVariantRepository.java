package com.rudraksha.shopsphere.catalog.repository;

import com.rudraksha.shopsphere.catalog.entity.ProductVariant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends MongoRepository<ProductVariant, String> {
    
    List<ProductVariant> findByProductId(String productId);
    
    Optional<ProductVariant> findBySku(String sku);
    
    boolean existsBySku(String sku);
    
    List<ProductVariant> findByProductIdAndActive(String productId, boolean active);
}