package com.rudraksha.shopsphere.returns.service.impl;

import com.rudraksha.shopsphere.returns.dto.request.CreateReturnRequest;
import com.rudraksha.shopsphere.returns.dto.request.UpdateReturnRequest;
import com.rudraksha.shopsphere.returns.dto.response.ReturnResponse;
import com.rudraksha.shopsphere.returns.entity.Return;
import com.rudraksha.shopsphere.returns.kafka.ReturnEventProducer;
import com.rudraksha.shopsphere.returns.repository.ReturnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnServiceImplTest {

    @Mock
    private ReturnRepository returnRepository;
    @Mock
    private ReturnEventProducer returnEventProducer;

    @InjectMocks
    private ReturnServiceImpl returnService;

    private Return returnEntity;
    private String returnId = "ret-123";

    @BeforeEach
    void setUp() {
        returnEntity = Return.builder()
                .id(returnId)
                .orderId("ORD-123")
                .customerId("CUST-123")
                .refundAmount(BigDecimal.valueOf(50.00))
                .status(Return.ReturnStatus.INITIATED)
                .build();
    }

    @Test
    void createReturn_Success() {
        CreateReturnRequest request = new CreateReturnRequest();
        request.setOrderId("ORD-123");
        request.setCustomerId("CUST-123");
        request.setRefundAmount(BigDecimal.valueOf(50.00));
        request.setReason("DAMAGED");

        when(returnRepository.save(any(Return.class))).thenReturn(returnEntity);

        ReturnResponse response = returnService.createReturn(request);

        assertNotNull(response);
        assertEquals(Return.ReturnStatus.INITIATED, response.getStatus());
        verify(returnRepository).save(any(Return.class));
        verify(returnEventProducer).publishReturnEvent(any(), eq("RETURN_INITIATED"));
    }

    @Test
    void getReturnById_Success() {
        when(returnRepository.findById(returnId)).thenReturn(Optional.of(returnEntity));

        ReturnResponse response = returnService.getReturnById(returnId);

        assertNotNull(response);
        assertEquals(returnId, response.getId());
    }

    @Test
    void updateReturn_Success() {
        UpdateReturnRequest request = new UpdateReturnRequest();
        request.setStatus(Return.ReturnStatus.RECEIVED);

        when(returnRepository.findById(returnId)).thenReturn(Optional.of(returnEntity));
        when(returnRepository.save(any(Return.class))).thenReturn(returnEntity);

        ReturnResponse response = returnService.updateReturn(returnId, request);

        assertNotNull(response);
        assertEquals(Return.ReturnStatus.RECEIVED, returnEntity.getStatus());
        verify(returnEventProducer).publishReturnEvent(any(), eq("RETURN_UPDATED"));
    }
}
