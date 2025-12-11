package com.rudraksha.shopsphere.inventory.repository;

import com.rudraksha.shopsphere.inventory.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findBySku(String sku);
    
    Optional<InventoryItem> findByProductId(Long productId);
    
    List<InventoryItem> findByStatus(InventoryItem.InventoryStatus status);
    
    @Query("SELECT ii FROM InventoryItem ii WHERE ii.quantity <= ii.reorderLevel")
    List<InventoryItem> findLowStockItems();
    
    @Query("SELECT ii FROM InventoryItem ii WHERE ii.quantity - ii.reservedQuantity >= :requiredQuantity")
    List<InventoryItem> findItemsWithSufficientStock(@Param("requiredQuantity") Integer requiredQuantity);
}
