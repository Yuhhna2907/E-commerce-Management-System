package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.OrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {
    List<OrderHistory> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    List<OrderHistory> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
