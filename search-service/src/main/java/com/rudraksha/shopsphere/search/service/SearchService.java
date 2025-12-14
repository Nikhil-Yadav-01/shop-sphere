package com.rudraksha.shopsphere.search.service;

import com.rudraksha.shopsphere.search.dto.request.SearchRequest;
import com.rudraksha.shopsphere.search.dto.response.SearchResponse;
import com.rudraksha.shopsphere.search.entity.SearchIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SearchService {
    Page<SearchResponse> search(SearchRequest request, Pageable pageable);

    Page<SearchResponse> searchByCategory(String categoryId, Pageable pageable);

    Page<SearchResponse> searchByStatus(String status, Pageable pageable);

    Page<SearchResponse> searchByPriceRange(java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, Pageable pageable);

    Page<SearchResponse> searchByInStock(Boolean inStock, Pageable pageable);

    void indexProduct(SearchIndex searchIndex);

    void deleteIndexByProductId(String productId);

    void updateIndexByProductId(String productId, SearchIndex searchIndex);

    long getIndexSize();

    void reindexAll();
}
