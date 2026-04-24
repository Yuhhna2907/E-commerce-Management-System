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
    // --- ANALYTICS QUERIES ---

    @Query("SELECT oi.product.id, SUM(oi.quantity), SUM(oi.price * oi.quantity) " +
           "FROM OrderItem oi JOIN oi.order o " +
           "WHERE o.status = com.codegym.smartphonemanagement.model.OrderStatus.DELIVERED " +
           "AND o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "GROUP BY oi.product.id")
    List<Object[]> sumSalesByProductInPeriod(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT oi.product.category.id, SUM(oi.quantity), SUM(oi.price * oi.quantity) " +
           "FROM OrderItem oi JOIN oi.order o " +
           "WHERE o.status = com.codegym.smartphonemanagement.model.OrderStatus.DELIVERED " +
           "AND o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "GROUP BY oi.product.category.id")
    List<Object[]> sumSalesByCategoryInPeriod(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT oi.product.brand, SUM(oi.quantity), SUM(oi.price * oi.quantity) " +
           "FROM OrderItem oi JOIN oi.order o " +
           "WHERE o.status = com.codegym.smartphonemanagement.model.OrderStatus.DELIVERED " +
           "AND o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "GROUP BY oi.product.brand")
    List<Object[]> sumSalesByBrandInPeriod(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    // --- REPEAT PURCHASE & CROSS SELL ---

    // Đếm số người mua lại cùng một sản phẩm
    @Query("SELECT oi.product.id, COUNT(DISTINCT o.user.id) " +
           "FROM OrderItem oi JOIN oi.order o " +
           "WHERE o.status = com.codegym.smartphonemanagement.model.OrderStatus.DELIVERED " +
           "GROUP BY oi.product.id " +
           "HAVING COUNT(o.id) > 1")
    List<Object[]> findRepeatPurchaseProducts();

    // Tìm các sản phẩm thường được mua cùng nhau (Cross-sell)
    @Query("SELECT oi1.product.name, oi2.product.name, COUNT(o.id) as freq " +
           "FROM OrderItem oi1 JOIN oi1.order o JOIN OrderItem oi2 ON oi1.order.id = oi2.order.id " +
           "WHERE oi1.product.id < oi2.product.id " +
           "AND o.status = com.codegym.smartphonemanagement.model.OrderStatus.DELIVERED " +
           "GROUP BY oi1.product.id, oi2.product.id " +
           "ORDER BY freq DESC")
    List<Object[]> findTopCrossSellPairs(org.springframework.data.domain.Pageable pageable);

    // Top sản phẩm bán chạy trong khoảng thời gian
    @Query("SELECT oi.product.id, SUM(oi.quantity) " +
           "FROM OrderItem oi JOIN oi.order o " +
           "WHERE o.status = com.codegym.smartphonemanagement.model.OrderStatus.DELIVERED " +
           "AND o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "GROUP BY oi.product.id " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> sumTopSellingProductsInPeriod(@Param("startDate") java.time.LocalDateTime startDate, 
                                                   @Param("endDate") java.time.LocalDateTime endDate, 
                                                   org.springframework.data.domain.Pageable pageable);

    // --- PHÂN KHÚC KHÁCH HÀNG (VIP vs NEW) ---

    @Query("SELECT oi.product.id, SUM(oi.quantity) " +
           "FROM OrderItem oi JOIN oi.order o JOIN LoyaltyAccount la ON o.user.id = la.user.id " +
           "WHERE o.status = com.codegym.smartphonemanagement.model.OrderStatus.DELIVERED " +
           "AND la.lifetimePoints >= :minLifetimePoints " +
           "GROUP BY oi.product.id " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findVipCustomerFavorites(@Param("minLifetimePoints") int minLifetimePoints, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT oi.product.id, SUM(oi.quantity) " +
           "FROM OrderItem oi JOIN oi.order o JOIN LoyaltyAccount la ON o.user.id = la.user.id " +
           "WHERE o.status = com.codegym.smartphonemanagement.model.OrderStatus.DELIVERED " +
           "AND la.lifetimePoints < :maxLifetimePoints " +
           "GROUP BY oi.product.id " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findNewCustomerFavorites(@Param("maxLifetimePoints") int maxLifetimePoints, org.springframework.data.domain.Pageable pageable);

    // --- BIỂU ĐỒ TĂNG TRƯỞNG DOANH SỐ SP ---
    @Query("SELECT DATE(o.createdAt), SUM(oi.price * oi.quantity) " +
           "FROM OrderItem oi JOIN oi.order o " +
           "WHERE o.status != 'CANCELLED' " +
           "AND o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "GROUP BY DATE(o.createdAt) ORDER BY DATE(o.createdAt) ASC")
    List<Object[]> getSalesGrowthTrend(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);
}
