package com.rudraksha.shopsphere.returns.repository;

import com.rudraksha.shopsphere.returns.entity.Return;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReturnRepository extends MongoRepository<Return, String> {
    Page<Return> findByOrderId(String orderId, Pageable pageable);
    Page<Return> findByCustomerId(String customerId, Pageable pageable);
    Page<Return> findByStatus(Return.ReturnStatus status, Pageable pageable);
}
