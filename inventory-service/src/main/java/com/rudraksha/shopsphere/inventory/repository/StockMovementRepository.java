package com.rudraksha.shopsphere.inventory.repository;

import com.rudraksha.shopsphere.inventory.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByInventoryItemId(Long inventoryItemId);
    
    List<StockMovement> findByMovementType(StockMovement.MovementType movementType);
    
    List<StockMovement> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
