package com.rudraksha.shopsphere.catalog.repository;

import com.rudraksha.shopsphere.catalog.entity.Product;
import com.rudraksha.shopsphere.catalog.entity.Product.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findBySku(String sku);
    Page<Product> findByCategoryId(String categoryId, Pageable pageable);
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    
    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    Page<Product> searchByName(String name, Pageable pageable);
    
    boolean existsBySku(String sku);
}
