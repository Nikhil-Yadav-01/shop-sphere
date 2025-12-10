package com.rudraksha.shopsphere.checkout.repository;

import com.rudraksha.shopsphere.checkout.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByUserId(String userId);
    Page<Order> findByUserId(String userId, Pageable pageable);
    List<Order> findByStatus(Order.OrderStatus status);
}