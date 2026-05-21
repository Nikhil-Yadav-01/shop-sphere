package com.rudraksha.shopsphere.catalog.service.impl;

import com.rudraksha.shopsphere.catalog.dto.request.CreateProductRequest;
import com.rudraksha.shopsphere.catalog.dto.request.UpdateProductRequest;
import com.rudraksha.shopsphere.catalog.dto.response.ProductResponse;
import com.rudraksha.shopsphere.catalog.entity.Product;
import com.rudraksha.shopsphere.catalog.entity.Product.ProductStatus;
import com.rudraksha.shopsphere.catalog.kafka.ProductEventProducer;
import com.rudraksha.shopsphere.catalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductEventProducer productEventProducer;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private String productId = "prod-123";
    private String sku = "SKU-123";

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(productId)
                .sku(sku)
                .name("Test Product")
                .price(BigDecimal.valueOf(100.00))
                .status(ProductStatus.ACTIVE)
                .build();
    }

    @Test
    void createProduct_Success() {
        CreateProductRequest request = new CreateProductRequest();
        request.setSku(sku);
        request.setName("Test Product");
        request.setPrice(BigDecimal.valueOf(100.00));

        when(productRepository.existsBySku(sku)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals(sku, response.getSku());
        verify(productRepository).save(any(Product.class));
        verify(productEventProducer).publishProductCreated(any(Product.class));
    }

    @Test
    void createProduct_DuplicateSku() {
        CreateProductRequest request = new CreateProductRequest();
        request.setSku(sku);

        when(productRepository.existsBySku(sku)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(request));
    }

    @Test
    void getProductById_Success() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(productId);

        assertNotNull(response);
        assertEquals(productId, response.getId());
    }

    @Test
    void getProductById_NotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> productService.getProductById(productId));
    }

    @Test
    void updateProduct_Success() {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Updated Name");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.updateProduct(productId, request);

        assertNotNull(response);
        verify(productRepository).save(any(Product.class));
        verify(productEventProducer).publishProductUpdated(any(Product.class));
    }

    @Test
    void deleteProduct_Success() {
        when(productRepository.existsById(productId)).thenReturn(true);
        doNothing().when(productRepository).deleteById(productId);

        assertDoesNotThrow(() -> productService.deleteProduct(productId));
        verify(productRepository).deleteById(productId);
        verify(productEventProducer).publishProductDeleted(productId);
    }

    @Test
    void getAllProducts_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(Collections.singletonList(product));
        when(productRepository.findAll(pageable)).thenReturn(page);

        Page<ProductResponse> response = productService.getAllProducts(pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }
}
