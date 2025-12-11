package com.rudraksha.shopsphere.inventory.controller;

import com.rudraksha.shopsphere.inventory.dto.request.*;
import com.rudraksha.shopsphere.inventory.dto.response.InventoryResponse;
import com.rudraksha.shopsphere.inventory.dto.response.StockMovementResponse;
import com.rudraksha.shopsphere.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryResponse> createInventory(@Valid @RequestBody CreateInventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(inventoryService.createInventory(request));
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<InventoryResponse> getInventoryBySku(@PathVariable String sku) {
        return ResponseEntity.ok(inventoryService.getInventoryBySku(sku));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryResponse> getInventoryByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryResponse> updateInventory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInventoryRequest request) {
        return ResponseEntity.ok(inventoryService.updateInventory(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInventory(@PathVariable Long id) {
        inventoryService.deleteInventory(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reserve")
    public ResponseEntity<InventoryResponse> reserveInventory(@Valid @RequestBody ReserveInventoryRequest request) {
        return ResponseEntity.ok(inventoryService.reserveInventory(request));
    }

    @PostMapping("/release-reservation")
    public ResponseEntity<InventoryResponse> releaseReservation(
            @RequestParam String sku,
            @RequestParam Integer quantity,
            @RequestParam String reference) {
        return ResponseEntity.ok(inventoryService.releaseReservation(sku, quantity, reference));
    }

    @PutMapping("/{id}/adjust")
    public ResponseEntity<InventoryResponse> adjustInventory(
            @PathVariable Long id,
            @Valid @RequestBody AdjustInventoryRequest request) {
        return ResponseEntity.ok(inventoryService.adjustInventory(id, request));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> getLowStockItems() {
        return ResponseEntity.ok(inventoryService.getLowStockItems());
    }

    @GetMapping("/out-of-stock")
    public ResponseEntity<List<InventoryResponse>> getOutOfStockItems() {
        return ResponseEntity.ok(inventoryService.getOutOfStockItems());
    }

    @GetMapping("/{id}/movements")
    public ResponseEntity<List<StockMovementResponse>> getMovementHistory(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getStockMovementHistory(id));
    }

    @GetMapping("/check-availability")
    public ResponseEntity<Boolean> checkAvailability(
            @RequestParam String sku,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(inventoryService.checkAvailability(sku, quantity));
    }
}
