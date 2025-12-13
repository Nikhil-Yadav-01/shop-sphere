package com.rudraksha.shopsphere.returns.service.impl;

import com.rudraksha.shopsphere.returns.dto.request.CreateReturnRequest;
import com.rudraksha.shopsphere.returns.dto.request.UpdateReturnRequest;
import com.rudraksha.shopsphere.returns.dto.response.ReturnResponse;
import com.rudraksha.shopsphere.returns.entity.Return;
import com.rudraksha.shopsphere.returns.exception.InvalidReturnException;
import com.rudraksha.shopsphere.returns.kafka.ReturnEventProducer;
import com.rudraksha.shopsphere.returns.repository.ReturnRepository;
import com.rudraksha.shopsphere.returns.service.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReturnServiceImpl implements ReturnService {

    private final ReturnRepository returnRepository;
    private final ReturnEventProducer returnEventProducer;

    @Override
    public ReturnResponse createReturn(CreateReturnRequest request) {
        // Validate refund amount
        if (request.getRefundAmount() == null) {
            throw new InvalidReturnException("Refund amount is required");
        }
        if (request.getRefundAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidReturnException("Refund amount must be greater than or equal to 0");
        }
        Return returnEntity = Return.builder()
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .reason(request.getReason())
                .description(request.getDescription())
                .itemIds(request.getItemIds())
                .refundAmount(request.getRefundAmount())
                .status(Return.ReturnStatus.INITIATED)
                .build();

        Return savedReturn = returnRepository.save(returnEntity);
        returnEventProducer.publishReturnEvent(savedReturn, "RETURN_INITIATED");
        return mapToResponse(savedReturn);
    }

    @Override
    public ReturnResponse getReturnById(String id) {
        Return returnEntity = returnRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Return not found with id: " + id));
        return mapToResponse(returnEntity);
    }

    @Override
    public ReturnResponse updateReturn(String id, UpdateReturnRequest request) {
        Return returnEntity = returnRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Return not found with id: " + id));

        if (request.getStatus() != null) {
            returnEntity.setStatus(request.getStatus());
        }
        if (request.getTrackingNumber() != null) {
            returnEntity.setTrackingNumber(request.getTrackingNumber());
        }
        if (request.getDescription() != null) {
            returnEntity.setDescription(request.getDescription());
        }

        Return updatedReturn = returnRepository.save(returnEntity);
        returnEventProducer.publishReturnEvent(updatedReturn, "RETURN_UPDATED");
        return mapToResponse(updatedReturn);
    }

    @Override
    public void deleteReturn(String id) {
        Return returnEntity = returnRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Return not found with id: " + id));
        returnRepository.delete(returnEntity);
        returnEventProducer.publishReturnEvent(returnEntity, "RETURN_DELETED");
    }

    @Override
    public Page<ReturnResponse> getAllReturns(Pageable pageable) {
        return returnRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    public Page<ReturnResponse> getReturnsByOrderId(String orderId, Pageable pageable) {
        return returnRepository.findByOrderId(orderId, pageable).map(this::mapToResponse);
    }

    @Override
    public Page<ReturnResponse> getReturnsByCustomerId(String customerId, Pageable pageable) {
        return returnRepository.findByCustomerId(customerId, pageable).map(this::mapToResponse);
    }

    @Override
    public Page<ReturnResponse> getReturnsByStatus(Return.ReturnStatus status, Pageable pageable) {
        return returnRepository.findByStatus(status, pageable).map(this::mapToResponse);
    }

    private ReturnResponse mapToResponse(Return returnEntity) {
        return ReturnResponse.builder()
                .id(returnEntity.getId())
                .orderId(returnEntity.getOrderId())
                .customerId(returnEntity.getCustomerId())
                .reason(returnEntity.getReason())
                .description(returnEntity.getDescription())
                .itemIds(returnEntity.getItemIds())
                .refundAmount(returnEntity.getRefundAmount())
                .status(returnEntity.getStatus())
                .trackingNumber(returnEntity.getTrackingNumber())
                .createdAt(returnEntity.getCreatedAt())
                .updatedAt(returnEntity.getUpdatedAt())
                .build();
    }
}
