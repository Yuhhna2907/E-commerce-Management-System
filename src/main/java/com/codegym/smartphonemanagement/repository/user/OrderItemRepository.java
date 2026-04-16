package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
    SELECT COUNT(oi) > 0
    FROM OrderItem oi
    WHERE oi.order.user.id = :userId
      AND oi.product.id = :productId
      AND oi.order.status = com.codegym.smartphonemanagement.model.OrderStatus.DELIVERED
""")
    boolean existsCompletedPurchase(Long userId, Long productId);

    /**
     * Đếm số đơn hàng riêng biệt chứa sản phẩm (chỉ tính đơn đã DELIVERED)
     * Dùng cho tính toán tần suất mua chung trong recommendation engine
     * 
     * @param productId ID của sản phẩm cần đếm
     * @return Số lượng đơn hàng chứa sản phẩm này
     */
    @Query("SELECT COUNT(DISTINCT oi.order.id) FROM OrderItem oi " +
           "WHERE oi.product.id = :productId " +
           "AND oi.order.status = com.codegym.smartphonemanagement.model.OrderStatus.DELIVERED")
    int countDistinctOrdersByProductId(@Param("productId") Long productId);

    /**
     * Tìm các sản phẩm được mua cùng với productId và đếm số lần mua chung (chỉ tính đơn đã DELIVERED)
     * Dùng cho recommendation engine để xác định co-purchase patterns
     * 
     * Trả về danh sách Object[] với format: [product_id (Long), count (Long)]
     * - product_id: ID của sản phẩm được mua cùng
     * - count: Số đơn hàng chứa cả hai sản phẩm
     * 
     * @param productId ID của sản phẩm gốc
     * @return Danh sách [product_id, count] của các sản phẩm mua chung
     */
    @Query("SELECT oi2.product.id, COUNT(DISTINCT oi2.order.id) " +
           "FROM OrderItem oi1 " +
           "JOIN OrderItem oi2 ON oi1.order.id = oi2.order.id " +
           "WHERE oi1.product.id = :productId " +
           "AND oi2.product.id != :productId " +
           "AND oi1.order.status = com.codegym.smartphonemanagement.model.OrderStatus.DELIVERED " +
           "GROUP BY oi2.product.id")
    List<Object[]> findCoPurchasedProducts(@Param("productId") Long productId);
}
