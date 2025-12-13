package com.rudraksha.shopsphere.returns.service;

import com.rudraksha.shopsphere.returns.dto.request.CreateReturnRequest;
import com.rudraksha.shopsphere.returns.dto.request.UpdateReturnRequest;
import com.rudraksha.shopsphere.returns.dto.response.ReturnResponse;
import com.rudraksha.shopsphere.returns.entity.Return;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReturnService {
    ReturnResponse createReturn(CreateReturnRequest request);
    ReturnResponse getReturnById(String id);
    ReturnResponse updateReturn(String id, UpdateReturnRequest request);
    void deleteReturn(String id);
    Page<ReturnResponse> getAllReturns(Pageable pageable);
    Page<ReturnResponse> getReturnsByOrderId(String orderId, Pageable pageable);
    Page<ReturnResponse> getReturnsByCustomerId(String customerId, Pageable pageable);
    Page<ReturnResponse> getReturnsByStatus(Return.ReturnStatus status, Pageable pageable);
}
