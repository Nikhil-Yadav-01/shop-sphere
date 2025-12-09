package com.rudraksha.shopsphere.catalog.service;

import com.rudraksha.shopsphere.catalog.dto.request.CreateProductRequest;
import com.rudraksha.shopsphere.catalog.dto.request.UpdateProductRequest;
import com.rudraksha.shopsphere.catalog.dto.response.ProductResponse;
import com.rudraksha.shopsphere.catalog.entity.Product.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductResponse createProduct(CreateProductRequest request);
    ProductResponse getProductById(String id);
    ProductResponse getProductBySku(String sku);
    ProductResponse updateProduct(String id, UpdateProductRequest request);
    void deleteProduct(String id);
    Page<ProductResponse> getAllProducts(Pageable pageable);
    Page<ProductResponse> getProductsByCategory(String categoryId, Pageable pageable);
    Page<ProductResponse> getProductsByStatus(ProductStatus status, Pageable pageable);
    Page<ProductResponse> searchProducts(String keyword, Pageable pageable);
}
