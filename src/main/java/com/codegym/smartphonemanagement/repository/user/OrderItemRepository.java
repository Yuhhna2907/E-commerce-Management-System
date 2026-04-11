package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
    SELECT COUNT(oi) > 0
    FROM OrderItem oi
    WHERE oi.order.user.id = :userId
      AND oi.product.id = :productId
      AND oi.order.status IN (
          com.codegym.smartphonemanagement.model.OrderStatus.PAID,
          com.codegym.smartphonemanagement.model.OrderStatus.SHIPPED
      )
""")
    boolean existsCompletedPurchase(Long userId, Long productId);
}
