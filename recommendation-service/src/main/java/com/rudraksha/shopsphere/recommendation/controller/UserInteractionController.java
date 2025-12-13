package com.rudraksha.shopsphere.recommendation.controller;

import com.rudraksha.shopsphere.recommendation.dto.request.UserInteractionRequest;
import com.rudraksha.shopsphere.recommendation.dto.response.UserInteractionResponse;
import com.rudraksha.shopsphere.recommendation.service.UserInteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
public class UserInteractionController {

    private final UserInteractionService userInteractionService;

    @PostMapping
    public ResponseEntity<UserInteractionResponse> recordInteraction(
            @Valid @RequestBody UserInteractionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userInteractionService.recordInteraction(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserInteractionResponse> getInteractionById(@PathVariable String id) {
        return ResponseEntity.ok(userInteractionService.getInteractionById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserInteractionResponse>> getUserInteractions(@PathVariable String userId) {
        return ResponseEntity.ok(userInteractionService.getUserInteractions(userId));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<UserInteractionResponse>> getProductInteractions(@PathVariable String productId) {
        return ResponseEntity.ok(userInteractionService.getProductInteractions(productId));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<UserInteractionResponse>> getInteractionsByType(@PathVariable String type) {
        return ResponseEntity.ok(userInteractionService.getInteractionsByType(type));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserInteractionResponse> updateInteraction(
            @PathVariable String id,
            @Valid @RequestBody UserInteractionRequest request) {
        return ResponseEntity.ok(userInteractionService.updateInteraction(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInteraction(@PathVariable String id) {
        userInteractionService.deleteInteraction(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteUserInteractions(@PathVariable String userId) {
        userInteractionService.deleteUserInteractions(userId);
        return ResponseEntity.noContent().build();
    }
}
