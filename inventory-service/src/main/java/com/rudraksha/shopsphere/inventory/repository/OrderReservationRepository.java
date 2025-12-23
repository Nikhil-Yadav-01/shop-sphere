package com.rudraksha.shopsphere.inventory.repository;

import com.rudraksha.shopsphere.inventory.entity.OrderReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderReservationRepository extends JpaRepository<OrderReservation, Long> {
    List<OrderReservation> findByOrderId(String orderId);
    
    void deleteByOrderId(String orderId);
}
