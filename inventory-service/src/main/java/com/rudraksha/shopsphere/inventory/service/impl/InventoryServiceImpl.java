package com.rudraksha.shopsphere.inventory.service.impl;

import com.rudraksha.shopsphere.inventory.dto.request.*;
import com.rudraksha.shopsphere.inventory.dto.response.InventoryResponse;
import com.rudraksha.shopsphere.inventory.dto.response.StockMovementResponse;
import com.rudraksha.shopsphere.inventory.entity.InventoryItem;
import com.rudraksha.shopsphere.inventory.entity.OrderReservation;
import com.rudraksha.shopsphere.inventory.entity.StockMovement;
import com.rudraksha.shopsphere.inventory.repository.InventoryItemRepository;
import com.rudraksha.shopsphere.inventory.repository.OrderReservationRepository;
import com.rudraksha.shopsphere.inventory.repository.StockMovementRepository;
import com.rudraksha.shopsphere.inventory.service.InventoryService;
import com.rudraksha.shopsphere.inventory.util.DistributedLockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {
    private final InventoryItemRepository inventoryRepository;
    private final StockMovementRepository movementRepository;
    private final OrderReservationRepository orderReservationRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final DistributedLockUtil distributedLockUtil;

    @Override
    @Transactional
    public InventoryResponse createInventory(CreateInventoryRequest request) {
        log.info("Creating inventory for SKU: {}", request.getSku());
        
        InventoryItem item = InventoryItem.builder()
            .sku(request.getSku())
            .productId(request.getProductId())
            .quantity(request.getQuantity())
            .reorderLevel(request.getReorderLevel())
            .warehouseLocation(request.getWarehouseLocation())
            .build();
        
        InventoryItem saved = inventoryRepository.save(item);
        
        recordMovement(saved.getId(), StockMovement.MovementType.INBOUND, 
            request.getQuantity(), "Initial inventory", null);
        
        log.info("Inventory created successfully for SKU: {}", request.getSku());
        return InventoryResponse.fromEntity(saved);
    }

    @Override
    public InventoryResponse getInventoryBySku(String sku) {
        log.info("Fetching inventory for SKU: {}", sku);
        InventoryItem item = inventoryRepository.findBySku(sku)
            .orElseThrow(() -> new IllegalArgumentException("Inventory not found for SKU: " + sku));
        return InventoryResponse.fromEntity(item);
    }

    @Override
    public InventoryResponse getInventoryByProductId(Long productId) {
        log.info("Fetching inventory for Product ID: {}", productId);
        InventoryItem item = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new IllegalArgumentException("Inventory not found for Product ID: " + productId));
        return InventoryResponse.fromEntity(item);
    }

    @Override
    @Transactional
    public InventoryResponse updateInventory(Long id, UpdateInventoryRequest request) {
        log.info("Updating inventory with ID: {}", id);
        InventoryItem item = inventoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));
        
        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
        }
        if (request.getReorderLevel() != null) {
            item.setReorderLevel(request.getReorderLevel());
        }
        if (request.getWarehouseLocation() != null) {
            item.setWarehouseLocation(request.getWarehouseLocation());
        }
        
        InventoryItem updated = inventoryRepository.save(item);
        log.info("Inventory updated successfully");
        return InventoryResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteInventory(Long id) {
        log.info("Deleting inventory with ID: {}", id);
        inventoryRepository.deleteById(id);
    }

    @Override
    @Transactional
    public InventoryResponse reserveInventory(ReserveInventoryRequest request) {
        log.info("Reserving inventory for SKU: {} with quantity: {}", 
            request.getSku(), request.getQuantity());
        
        InventoryItem item = inventoryRepository.findBySku(request.getSku())
            .orElseThrow(() -> new IllegalArgumentException("Inventory not found for SKU: " + request.getSku()));
        
        if (item.getAvailableQuantity() < request.getQuantity()) {
            log.warn("Insufficient inventory for SKU: {}", request.getSku());
            throw new IllegalArgumentException("Insufficient inventory available");
        }
        
        item.setReservedQuantity(item.getReservedQuantity() + request.getQuantity());
        updateStatus(item);
        
        InventoryItem updated = inventoryRepository.save(item);
        recordMovement(item.getId(), StockMovement.MovementType.RESERVATION, 
            request.getQuantity(), request.getReference(), null);
        
        publishEvent("inventory.reserved", request.getSku(), request.getQuantity());
        log.info("Inventory reserved successfully for SKU: {}", request.getSku());
        
        return InventoryResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public InventoryResponse releaseReservation(String sku, Integer quantity, String reference) {
        log.info("Releasing reservation for SKU: {} with quantity: {}", sku, quantity);
        
        InventoryItem item = inventoryRepository.findBySku(sku)
            .orElseThrow(() -> new IllegalArgumentException("Inventory not found for SKU: " + sku));
        
        int newReserved = Math.max(0, item.getReservedQuantity() - quantity);
        item.setReservedQuantity(newReserved);
        updateStatus(item);
        
        InventoryItem updated = inventoryRepository.save(item);
        recordMovement(item.getId(), StockMovement.MovementType.RESERVATION_RELEASE, 
            quantity, reference, null);
        
        publishEvent("inventory.reservation.released", sku, quantity);
        log.info("Reservation released successfully for SKU: {}", sku);
        
        return InventoryResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public InventoryResponse adjustInventory(Long id, AdjustInventoryRequest request) {
        log.info("Adjusting inventory with ID: {} by quantity: {}", id, request.getAdjustmentQuantity());
        
        InventoryItem item = inventoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));
        
        item.setQuantity(item.getQuantity() + request.getAdjustmentQuantity());
        updateStatus(item);
        
        InventoryItem updated = inventoryRepository.save(item);
        recordMovement(id, StockMovement.MovementType.ADJUSTMENT, 
            request.getAdjustmentQuantity(), request.getReason(), request.getNotes());
        
        log.info("Inventory adjusted successfully");
        return InventoryResponse.fromEntity(updated);
    }

    @Override
    public List<InventoryResponse> getLowStockItems() {
        log.info("Fetching low stock items");
        return inventoryRepository.findLowStockItems().stream()
            .map(InventoryResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<InventoryResponse> getOutOfStockItems() {
        log.info("Fetching out of stock items");
        return inventoryRepository.findByStatus(InventoryItem.InventoryStatus.OUT_OF_STOCK).stream()
            .map(InventoryResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<StockMovementResponse> getStockMovementHistory(Long inventoryItemId) {
        log.info("Fetching stock movement history for inventory item: {}", inventoryItemId);
        return movementRepository.findByInventoryItemId(inventoryItemId).stream()
            .map(StockMovementResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Override
    public boolean checkAvailability(String sku, Integer requiredQuantity) {
        return inventoryRepository.findBySku(sku)
            .map(item -> item.getAvailableQuantity() >= requiredQuantity)
            .orElse(false);
    }

    private void recordMovement(Long inventoryItemId, StockMovement.MovementType type, 
                               Integer quantity, String reference, String notes) {
        StockMovement movement = StockMovement.builder()
            .inventoryItemId(inventoryItemId)
            .movementType(type)
            .quantity(quantity)
            .reference(reference)
            .notes(notes)
            .build();
        movementRepository.save(movement);
        log.debug("Stock movement recorded: {} for inventory item: {}", type, inventoryItemId);
    }

    private void updateStatus(InventoryItem item) {
        if (item.getQuantity() <= 0) {
            item.setStatus(InventoryItem.InventoryStatus.OUT_OF_STOCK);
        } else if (item.getQuantity() <= item.getReorderLevel()) {
            item.setStatus(InventoryItem.InventoryStatus.LOW_STOCK);
        } else {
            item.setStatus(InventoryItem.InventoryStatus.AVAILABLE);
        }
    }

    private void publishEvent(String topic, String sku, Integer quantity) {
        try {
            String message = String.format("{\"sku\":\"%s\",\"quantity\":%d}", sku, quantity);
            kafkaTemplate.send(topic, sku, message);
            log.debug("Event published: {} for SKU: {}", topic, sku);
        } catch (Exception e) {
            log.warn("Failed to publish event: {} for SKU: {}", topic, sku, e);
        }
    }

    @Override
    @Transactional
    public InventoryResponse reserveInventoryForOrder(String sku, Integer quantity, String orderId) {
        log.info("Reserving inventory for order {} with SKU: {} and quantity: {}", orderId, sku, quantity);
        
        // Use distributed lock to prevent race conditions
        return distributedLockUtil.executeWithLock(sku, () -> {
            InventoryItem item = inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for SKU: " + sku));
            
            if (item.getAvailableQuantity() < quantity) {
                log.warn("Insufficient inventory for order {}: SKU {}", orderId, sku);
                throw new IllegalArgumentException("Insufficient inventory available for SKU: " + sku);
            }
            
            item.setReservedQuantity(item.getReservedQuantity() + quantity);
            updateStatus(item);
            
            InventoryItem updated = inventoryRepository.save(item);
            
            // Track order-to-inventory mapping for later release
            OrderReservation reservation = OrderReservation.builder()
                .orderId(orderId)
                .inventoryItemId(item.getId())
                .quantityReserved(quantity)
                .build();
            orderReservationRepository.save(reservation);
            
            recordMovement(item.getId(), StockMovement.MovementType.RESERVATION, 
                quantity, "Order: " + orderId, null);
            
            publishEvent("inventory.reserved", sku, quantity);
            log.info("Inventory reserved for order {}: SKU {} x {}", orderId, sku, quantity);
            
            return InventoryResponse.fromEntity(updated);
        });
    }

    @Override
    @Transactional
    public void releaseReservationByOrder(String orderId) {
        log.info("Releasing all reservations for order: {}", orderId);
        
        List<OrderReservation> reservations = orderReservationRepository.findByOrderId(orderId);
        
        for (OrderReservation reservation : reservations) {
            try {
                InventoryItem item = inventoryRepository.findById(reservation.getInventoryItemId())
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Inventory item not found: " + reservation.getInventoryItemId()));
                
                // Use lock for consistency
                distributedLockUtil.executeWithLock(item.getSku(), () -> {
                    InventoryItem currentItem = inventoryRepository.findById(item.getId())
                        .orElseThrow();
                    
                    int newReserved = Math.max(0, 
                        currentItem.getReservedQuantity() - reservation.getQuantityReserved());
                    currentItem.setReservedQuantity(newReserved);
                    updateStatus(currentItem);
                    
                    inventoryRepository.save(currentItem);
                    
                    recordMovement(item.getId(), StockMovement.MovementType.RESERVATION_RELEASE, 
                        reservation.getQuantityReserved(), "Order cancelled: " + orderId, null);
                    
                    publishEvent("inventory.reservation.released", item.getSku(), 
                        reservation.getQuantityReserved());
                    
                    log.info("Inventory released for order {}: {} x {}", orderId, item.getSku(), 
                        reservation.getQuantityReserved());
                    
                    return null;
                });
            } catch (Exception e) {
                log.error("Failed to release reservation for order {}", orderId, e);
                // Continue with next reservation even if one fails
            }
        }
        
        // Clean up order reservations after releasing
        orderReservationRepository.deleteByOrderId(orderId);
    }
}
