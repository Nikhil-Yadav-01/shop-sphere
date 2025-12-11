package com.rudraksha.shopsphere.fraud.controller;

import com.rudraksha.shopsphere.fraud.dto.FraudCheckRequest;
import com.rudraksha.shopsphere.fraud.dto.FraudCheckResponse;
import com.rudraksha.shopsphere.fraud.service.FraudDetectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fraud")
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionController {
    
    private final FraudDetectionService fraudDetectionService;
    
    @PostMapping("/check")
    public ResponseEntity<FraudCheckResponse> checkFraud(@Valid @RequestBody FraudCheckRequest request) {
        log.info("Received fraud check request for transaction: {}", request.getTransactionId());
        FraudCheckResponse response = fraudDetectionService.checkFraud(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<FraudCheckResponse> getFraudDetection(@PathVariable Long id) {
        FraudCheckResponse response = fraudDetectionService.getFraudDetectionById(id);
        if (response != null) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<FraudCheckResponse> getFraudDetectionByTransaction(@PathVariable String transactionId) {
        FraudCheckResponse response = fraudDetectionService.getFraudDetectionByTransactionId(transactionId);
        if (response != null) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/customer/{customerId}/history")
    public ResponseEntity<?> getCustomerFraudHistory(@PathVariable Long customerId) {
        var fraudHistory = fraudDetectionService.getCustomerFraudHistory(customerId);
        Map<String, Object> response = new HashMap<>();
        response.put("customerId", customerId);
        response.put("fraudulentTransactionCount", fraudHistory.size());
        response.put("transactions", fraudHistory);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "fraud-service");
        return ResponseEntity.ok(response);
    }
}
