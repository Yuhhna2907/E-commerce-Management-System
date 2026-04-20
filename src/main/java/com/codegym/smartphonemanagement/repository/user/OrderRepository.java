package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.Order;
import com.codegym.smartphonemanagement.model.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // --- GIỮ NGUYÊN CÁC HÀM CŨ CỦA NHÓM ---
    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUser_IdAndStatusIn(Long userId, Collection<OrderStatus> statuses);
    long countByStatus(OrderStatus status);
    List<Order> findAllByStatusOrderByCreatedAtDesc(OrderStatus status);
    List<Order> findAllByReceiverPhoneContaining(String phone);

    // 1. Tổng doanh thu hiện tại (Duy giữ lại cho nhóm dùng)
    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.status != 'CANCELLED'")
    BigDecimal calculateTotalRevenue();

    // 2. Tổng đơn thành công (DELIVERED hoặc PARTIAL_REFUNDED)
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'DELIVERED' OR o.status = 'PARTIAL_REFUNDED'")
    long countCompletedOrders();

    // --- THÊM MỚI CÁC HÀM PHỤC VỤ DASHBOARD CỦA DUY ---

    // A. Doanh thu THỰC TẾ (Chỉ tính đơn đã giao thành công)
    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.status = 'DELIVERED'")
    BigDecimal calculateActualRevenue();

    // B. Đếm đơn hàng ĐANG GIAO (Để hiển thị lên Stat Card mới)
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'SHIPPING'")
    long countShippingOrders();

    // C. Biểu đồ so sánh Doanh thu Thực tế vs Giả định (7 ngày)
    // actual: Status = DELIVERED
    // assumed: Status != CANCELLED
    @Query(value = "SELECT DATE(created_at) as date, " +
            "SUM(CASE WHEN status = 'DELIVERED' THEN total_price ELSE 0 END) as actual, " +
            "SUM(total_price) as assumed " +
            "FROM orders WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "GROUP BY DATE(created_at) ORDER BY date ASC", nativeQuery = true)
    List<Object[]> getRevenueComparisonLast7Days();

    // --- GIỮ NGUYÊN CÁC HÀM THỐNG KÊ CŨ ---

    @Query(value = "SELECT DATE(created_at) as date, SUM(total_price) as revenue " +
            "FROM orders WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "AND status != 'CANCELLED' " +
            "GROUP BY DATE(created_at) ORDER BY date ASC", nativeQuery = true)
    List<Object[]> getRevenueLast7Days();

    // D. Phân bổ nguồn tiền (VNPAY vs COD) của Mọi đơn hàng không Cancelled
    @Query("SELECT o.paymentMethod, COUNT(o) FROM Order o WHERE o.status != 'CANCELLED' GROUP BY o.paymentMethod")
    List<Object[]> getPaymentMethodDistribution();

    @Query("SELECT oi.product.name, SUM(oi.quantity) as totalSold " +
            "FROM OrderItem oi " +
            "JOIN oi.order o " +
            "WHERE o.status != 'CANCELLED' " +
            "GROUP BY oi.product.id, oi.product.name " +
            "ORDER BY totalSold DESC")
    List<Object[]> getTopSellingProducts(Pageable pageable);

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();

    // Performance optimization: Count orders by user without loading all orders
    long countByUserId(Long userId);

    // Performance optimization: Sum total price for non-cancelled orders without loading all orders
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.user.id = :userId AND o.status != :status")
    BigDecimal sumTotalPriceByUserIdAndStatusNot(@Param("userId") Long userId, @Param("status") OrderStatus status);

    // --- ADMIN ORDER CENTER: PAGINATED SEARCH + FILTER ---
    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "   CAST(o.id AS string) LIKE %:keyword% OR " +
           "   LOWER(o.receiverName) LIKE LOWER(CONCAT('%',:keyword,'%')) OR " +
           "   o.receiverPhone LIKE %:keyword%) AND " +
           "(:dateFrom IS NULL OR o.createdAt >= :dateFrom) AND " +
           "(:dateTo IS NULL OR o.createdAt <= :dateTo) " +
           "ORDER BY o.createdAt DESC")
    org.springframework.data.domain.Page<Order> searchOrders(
            @Param("status") OrderStatus status,
            @Param("keyword") String keyword,
            @Param("dateFrom") java.time.LocalDateTime dateFrom,
            @Param("dateTo") java.time.LocalDateTime dateTo,
            org.springframework.data.domain.Pageable pageable);
}