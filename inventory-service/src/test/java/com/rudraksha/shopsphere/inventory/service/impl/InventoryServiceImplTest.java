package com.rudraksha.shopsphere.inventory.service.impl;

import com.rudraksha.shopsphere.inventory.dto.request.CreateInventoryRequest;
import com.rudraksha.shopsphere.inventory.dto.request.ReserveInventoryRequest;
import com.rudraksha.shopsphere.inventory.dto.response.InventoryResponse;
import com.rudraksha.shopsphere.inventory.entity.InventoryItem;
import com.rudraksha.shopsphere.inventory.repository.InventoryItemRepository;
import com.rudraksha.shopsphere.inventory.repository.OrderReservationRepository;
import com.rudraksha.shopsphere.inventory.repository.OutboxEventRepository;
import com.rudraksha.shopsphere.inventory.repository.StockMovementRepository;
import com.rudraksha.shopsphere.inventory.util.DistributedLockUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;

import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryItemRepository inventoryRepository;
    @Mock
    private StockMovementRepository movementRepository;
    @Mock
    private OrderReservationRepository orderReservationRepository;
    @Mock
    private OutboxEventRepository outboxRepository;
    @Mock
    private DistributedLockUtil distributedLockUtil;
    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private InventoryItem inventoryItem;
    private String sku = "SKU-123";

    @BeforeEach
    void setUp() {
        inventoryItem = InventoryItem.builder()
                .id(1L)
                .sku(sku)
                .productId("PROD-123")
                .quantity(100)
                .reservedQuantity(0)
                .availableQuantity(100)
                .reorderLevel(10)
                .status(InventoryItem.InventoryStatus.AVAILABLE)
                .build();
    }

    @Test
    void createInventory_Success() {
        CreateInventoryRequest request = new CreateInventoryRequest();
        request.setSku(sku);
        request.setProductId("PROD-123");
        request.setQuantity(100);

        when(inventoryRepository.save(any(InventoryItem.class))).thenReturn(inventoryItem);

        InventoryResponse response = inventoryService.createInventory(request);

        assertNotNull(response);
        assertEquals(sku, response.getSku());
        verify(inventoryRepository).save(any(InventoryItem.class));
        verify(movementRepository).save(any());
    }

    @Test
    void getInventoryBySku_Success() {
        when(inventoryRepository.findBySku(sku)).thenReturn(Optional.of(inventoryItem));

        InventoryResponse response = inventoryService.getInventoryBySku(sku);

        assertNotNull(response);
        assertEquals(sku, response.getSku());
    }

    @Test
    void checkAvailability_True() {
        when(inventoryRepository.findBySku(sku)).thenReturn(Optional.of(inventoryItem));

        assertTrue(inventoryService.checkAvailability(sku, 50));
    }

    @Test
    void checkAvailability_False() {
        when(inventoryRepository.findBySku(sku)).thenReturn(Optional.of(inventoryItem));

        assertFalse(inventoryService.checkAvailability(sku, 150));
    }
}
