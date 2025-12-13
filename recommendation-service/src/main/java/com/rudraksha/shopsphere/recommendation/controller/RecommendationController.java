package com.rudraksha.shopsphere.recommendation.controller;

import com.rudraksha.shopsphere.recommendation.dto.request.CreateRecommendationRequest;
import com.rudraksha.shopsphere.recommendation.dto.response.RecommendationResponse;
import com.rudraksha.shopsphere.recommendation.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @PostMapping
    public ResponseEntity<RecommendationResponse> createRecommendation(
            @Valid @RequestBody CreateRecommendationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recommendationService.createRecommendation(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecommendationResponse> getRecommendationById(@PathVariable String id) {
        return ResponseEntity.ok(recommendationService.getRecommendationById(id));
    }

    @GetMapping
    public ResponseEntity<Page<RecommendationResponse>> getAllRecommendations(Pageable pageable) {
        return ResponseEntity.ok(recommendationService.getAllRecommendations(pageable));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<RecommendationResponse>> getUserRecommendations(
            @PathVariable String userId,
            Pageable pageable) {
        return ResponseEntity.ok(recommendationService.getUserRecommendations(userId, pageable));
    }

    @GetMapping("/user/{userId}/top")
    public ResponseEntity<List<RecommendationResponse>> getTopRecommendationsForUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(recommendationService.getTopRecommendationsForUser(userId, limit));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<Page<RecommendationResponse>> getRecommendationsByType(
            @PathVariable String type,
            Pageable pageable) {
        return ResponseEntity.ok(recommendationService.getRecommendationsByType(type, pageable));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Page<RecommendationResponse>> getRecommendationsByCategory(
            @PathVariable String category,
            Pageable pageable) {
        return ResponseEntity.ok(recommendationService.getRecommendationsByCategory(category, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecommendationResponse> updateRecommendation(
            @PathVariable String id,
            @Valid @RequestBody CreateRecommendationRequest request) {
        return ResponseEntity.ok(recommendationService.updateRecommendation(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecommendation(@PathVariable String id) {
        recommendationService.deleteRecommendation(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteUserRecommendations(@PathVariable String userId) {
        recommendationService.deleteUserRecommendations(userId);
        return ResponseEntity.noContent().build();
    }
}
