package com.rudraksha.shopsphere.returns.controller;

import com.rudraksha.shopsphere.returns.dto.request.CreateReturnRequest;
import com.rudraksha.shopsphere.returns.dto.request.UpdateReturnRequest;
import com.rudraksha.shopsphere.returns.dto.response.ReturnResponse;
import com.rudraksha.shopsphere.returns.entity.Return;
import com.rudraksha.shopsphere.returns.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    @PostMapping
    public ResponseEntity<ReturnResponse> createReturn(@Valid @RequestBody CreateReturnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(returnService.createReturn(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReturnResponse> getReturnById(@PathVariable String id) {
        return ResponseEntity.ok(returnService.getReturnById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReturnResponse> updateReturn(@PathVariable String id, @Valid @RequestBody UpdateReturnRequest request) {
        return ResponseEntity.ok(returnService.updateReturn(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReturn(@PathVariable String id) {
        returnService.deleteReturn(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<ReturnResponse>> getAllReturns(Pageable pageable) {
        return ResponseEntity.ok(returnService.getAllReturns(pageable));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Page<ReturnResponse>> getReturnsByOrderId(@PathVariable String orderId, Pageable pageable) {
        return ResponseEntity.ok(returnService.getReturnsByOrderId(orderId, pageable));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Page<ReturnResponse>> getReturnsByCustomerId(@PathVariable String customerId, Pageable pageable) {
        return ResponseEntity.ok(returnService.getReturnsByCustomerId(customerId, pageable));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<ReturnResponse>> getReturnsByStatus(@PathVariable Return.ReturnStatus status, Pageable pageable) {
        return ResponseEntity.ok(returnService.getReturnsByStatus(status, pageable));
    }
}
