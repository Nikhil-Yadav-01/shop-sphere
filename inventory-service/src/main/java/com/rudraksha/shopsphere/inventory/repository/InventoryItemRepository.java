package com.rudraksha.shopsphere.inventory.repository;

import com.rudraksha.shopsphere.inventory.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findBySku(String sku);
    Optional<InventoryItem> findByProductId(String productId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.sku = :sku")
    Optional<InventoryItem> findBySkuWithLock(String sku);
    
    @Query("SELECT i FROM InventoryItem i WHERE i.quantity <= i.reorderLevel")
    List<InventoryItem> findLowStockItems();
    
    List<InventoryItem> findByStatus(InventoryItem.InventoryStatus status);
}
