package com.rudraksha.shopsphere.search.service.impl;

import com.rudraksha.shopsphere.search.dto.request.SearchRequest;
import com.rudraksha.shopsphere.search.dto.response.SearchResponse;
import com.rudraksha.shopsphere.search.entity.SearchIndex;
import com.rudraksha.shopsphere.search.repository.SearchIndexRepository;
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
class SearchServiceImplTest {

    @Mock
    private SearchIndexRepository searchIndexRepository;

    @InjectMocks
    private SearchServiceImpl searchService;

    private SearchIndex searchIndex;
    private String productId = "prod-123";

    @BeforeEach
    void setUp() {
        searchIndex = SearchIndex.builder()
                .id("index-123")
                .productId(productId)
                .name("Test Product")
                .sku("SKU-123")
                .price(BigDecimal.valueOf(100.00))
                .status("ACTIVE")
                .build();
    }

    @Test
    void search_Success() {
        SearchRequest request = new SearchRequest();
        request.setKeyword("test");
        Pageable pageable = PageRequest.of(0, 10);
        Page<SearchIndex> page = new PageImpl<>(Collections.singletonList(searchIndex));

        when(searchIndexRepository.searchWithScore(eq("test"), eq(pageable))).thenReturn(page);

        Page<SearchResponse> response = searchService.search(request, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(productId, response.getContent().get(0).getProductId());
    }

    @Test
    void indexProduct_Success() {
        searchService.indexProduct(searchIndex);
        verify(searchIndexRepository).save(searchIndex);
    }

    @Test
    void deleteIndexByProductId_Success() {
        searchService.deleteIndexByProductId(productId);
        verify(searchIndexRepository).deleteByProductId(productId);
    }

    @Test
    void updateIndexByProductId_Success() {
        when(searchIndexRepository.findByProductId(productId)).thenReturn(Optional.of(searchIndex));
        
        searchService.updateIndexByProductId(productId, searchIndex);
        
        verify(searchIndexRepository).save(searchIndex);
    }

    @Test
    void searchByCategory_Success() {
        String categoryId = "cat-123";
        Pageable pageable = PageRequest.of(0, 10);
        Page<SearchIndex> page = new PageImpl<>(Collections.singletonList(searchIndex));

        when(searchIndexRepository.findByCategoryId(categoryId, pageable)).thenReturn(page);

        Page<SearchResponse> response = searchService.searchByCategory(categoryId, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }
}
