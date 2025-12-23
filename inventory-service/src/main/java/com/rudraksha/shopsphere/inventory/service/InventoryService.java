package com.rudraksha.shopsphere.inventory.service;

import com.rudraksha.shopsphere.inventory.dto.request.*;
import com.rudraksha.shopsphere.inventory.dto.response.InventoryResponse;
import com.rudraksha.shopsphere.inventory.dto.response.StockMovementResponse;

import java.util.List;

public interface InventoryService {
    InventoryResponse createInventory(CreateInventoryRequest request);
    
    InventoryResponse getInventoryBySku(String sku);
    
    InventoryResponse getInventoryByProductId(Long productId);
    
    InventoryResponse updateInventory(Long id, UpdateInventoryRequest request);
    
    void deleteInventory(Long id);
    
    InventoryResponse reserveInventory(ReserveInventoryRequest request);
    
    InventoryResponse releaseReservation(String sku, Integer quantity, String reference);
    
    InventoryResponse adjustInventory(Long id, AdjustInventoryRequest request);
    
    List<InventoryResponse> getLowStockItems();
    
    List<InventoryResponse> getOutOfStockItems();
    
    List<StockMovementResponse> getStockMovementHistory(Long inventoryItemId);
    
    boolean checkAvailability(String sku, Integer requiredQuantity);
    
    InventoryResponse reserveInventoryForOrder(String sku, Integer quantity, String orderId);
    
    void releaseReservationByOrder(String orderId);
}
