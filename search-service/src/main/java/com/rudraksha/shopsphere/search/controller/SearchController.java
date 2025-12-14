package com.rudraksha.shopsphere.search.controller;

import com.rudraksha.shopsphere.search.dto.request.SearchRequest;
import com.rudraksha.shopsphere.search.dto.response.SearchResponse;
import com.rudraksha.shopsphere.search.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping
    public ResponseEntity<Page<SearchResponse>> search(
            @Valid @RequestBody SearchRequest request,
            Pageable pageable) {
        return ResponseEntity.ok(searchService.search(request, pageable));
    }

    @GetMapping("/keyword")
    public ResponseEntity<Page<SearchResponse>> searchByKeyword(
            @RequestParam String keyword,
            Pageable pageable) {
        SearchRequest request = SearchRequest.builder()
                .keyword(keyword)
                .build();
        return ResponseEntity.ok(searchService.search(request, pageable));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<SearchResponse>> searchByCategory(
            @PathVariable String categoryId,
            Pageable pageable) {
        return ResponseEntity.ok(searchService.searchByCategory(categoryId, pageable));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<SearchResponse>> searchByStatus(
            @PathVariable String status,
            Pageable pageable) {
        return ResponseEntity.ok(searchService.searchByStatus(status, pageable));
    }

    @GetMapping("/price")
    public ResponseEntity<Page<SearchResponse>> searchByPrice(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice,
            Pageable pageable) {
        return ResponseEntity.ok(searchService.searchByPriceRange(minPrice, maxPrice, pageable));
    }

    @GetMapping("/in-stock/{inStock}")
    public ResponseEntity<Page<SearchResponse>> searchByInStock(
            @PathVariable Boolean inStock,
            Pageable pageable) {
        return ResponseEntity.ok(searchService.searchByInStock(inStock, pageable));
    }

    @GetMapping("/index-size")
    public ResponseEntity<Long> getIndexSize() {
        return ResponseEntity.ok(searchService.getIndexSize());
    }

    @PostMapping("/reindex")
    public ResponseEntity<String> reindexAll() {
        searchService.reindexAll();
        return ResponseEntity.ok("Reindexing started");
    }
}
