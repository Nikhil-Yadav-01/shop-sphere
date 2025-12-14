package com.rudraksha.shopsphere.search.service.impl;

import com.rudraksha.shopsphere.search.dto.request.SearchRequest;
import com.rudraksha.shopsphere.search.dto.response.SearchResponse;
import com.rudraksha.shopsphere.search.entity.SearchIndex;
import com.rudraksha.shopsphere.search.repository.SearchIndexRepository;
import com.rudraksha.shopsphere.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final SearchIndexRepository searchIndexRepository;

    @Override
    public Page<SearchResponse> search(SearchRequest request, Pageable pageable) {
        log.info("Searching with keyword: {}", request.getKeyword());

        Page<SearchIndex> results = searchIndexRepository.searchWithScore(request.getKeyword(), pageable);

        return results.map(this::mapToSearchResponse);
    }

    @Override
    public Page<SearchResponse> searchByCategory(String categoryId, Pageable pageable) {
        log.info("Searching by category: {}", categoryId);
        Page<SearchIndex> results = searchIndexRepository.findByCategoryId(categoryId, pageable);
        return results.map(this::mapToSearchResponse);
    }

    @Override
    public Page<SearchResponse> searchByStatus(String status, Pageable pageable) {
        log.info("Searching by status: {}", status);
        Page<SearchIndex> results = searchIndexRepository.findByStatus(status, pageable);
        return results.map(this::mapToSearchResponse);
    }

    @Override
    public Page<SearchResponse> searchByPriceRange(java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, Pageable pageable) {
        log.info("Searching by price range: {} - {}", minPrice, maxPrice);
        Page<SearchIndex> results = searchIndexRepository.findByPriceBetween(minPrice, maxPrice, pageable);
        return results.map(this::mapToSearchResponse);
    }

    @Override
    public Page<SearchResponse> searchByInStock(Boolean inStock, Pageable pageable) {
        log.info("Searching by inStock: {}", inStock);
        Page<SearchIndex> results = searchIndexRepository.findByInStock(inStock, pageable);
        return results.map(this::mapToSearchResponse);
    }

    @Override
    public void indexProduct(SearchIndex searchIndex) {
        log.info("Indexing product: {}", searchIndex.getProductId());
        searchIndex.setIndexedAt(System.currentTimeMillis());
        searchIndexRepository.save(searchIndex);
    }

    @Override
    public void deleteIndexByProductId(String productId) {
        log.info("Deleting index for product: {}", productId);
        searchIndexRepository.deleteByProductId(productId);
    }

    @Override
    public void updateIndexByProductId(String productId, SearchIndex searchIndex) {
        log.info("Updating index for product: {}", productId);
        SearchIndex existing = searchIndexRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Search index not found for product: " + productId));

        searchIndex.setId(existing.getId());
        searchIndex.setCreatedAt(existing.getCreatedAt());
        searchIndex.setIndexedAt(System.currentTimeMillis());
        searchIndexRepository.save(searchIndex);
    }

    @Override
    public long getIndexSize() {
        return searchIndexRepository.count();
    }

    @Override
    public void reindexAll() {
        log.info("Reindexing all products");
        // This would typically be called from a scheduled job or admin endpoint
        // and would fetch products from the catalog service to rebuild the index
    }

    private SearchResponse mapToSearchResponse(SearchIndex searchIndex) {
        return SearchResponse.builder()
                .id(searchIndex.getId())
                .productId(searchIndex.getProductId())
                .name(searchIndex.getName())
                .description(searchIndex.getDescription())
                .sku(searchIndex.getSku())
                .price(searchIndex.getPrice())
                .categoryId(searchIndex.getCategoryId())
                .categoryName(searchIndex.getCategoryName())
                .tags(searchIndex.getTags())
                .status(searchIndex.getStatus())
                .rating(searchIndex.getRating())
                .reviewCount(searchIndex.getReviewCount())
                .inStock(searchIndex.getInStock())
                .brand(searchIndex.getBrand())
                .createdAt(searchIndex.getCreatedAt())
                .updatedAt(searchIndex.getUpdatedAt())
                .build();
    }
}
