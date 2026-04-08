package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Tìm lịch sử đơn hàng của 1 user
    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}