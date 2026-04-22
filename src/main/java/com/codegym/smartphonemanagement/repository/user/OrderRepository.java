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
    // --- ANALYTICS PRO QUERIES ---

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate")
    long countOrdersInPeriod(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.status != 'CANCELLED' AND o.createdAt >= :startDate AND o.createdAt <= :endDate")
    BigDecimal sumGMVInPeriod(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.status = 'DELIVERED' AND o.createdAt >= :startDate AND o.createdAt <= :endDate")
    BigDecimal sumNetRevenueInPeriod(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(o.totalDiscount + o.shippingDiscount), 0) FROM Order o WHERE o.status != 'CANCELLED' AND o.createdAt >= :startDate AND o.createdAt <= :endDate")
    BigDecimal sumTotalDiscountInPeriod(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT o.status, COUNT(o) FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate GROUP BY o.status")
    List<Object[]> countOrdersByStatusInPeriod(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT o.shippingAddress, COUNT(o) " +
           "FROM Order o WHERE o.status != 'CANCELLED' AND o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "GROUP BY o.shippingAddress")
    List<Object[]> countOrdersByAddressInPeriod(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT p.category.name, SUM(oi.price * oi.quantity) FROM OrderItem oi JOIN oi.order o JOIN oi.product p " +
           "WHERE o.status != 'CANCELLED' AND o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "GROUP BY p.category.id, p.category.name ORDER BY SUM(oi.price * oi.quantity) DESC")
    List<Object[]> sumRevenueByCategoryInPeriod(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT p.brand, SUM(oi.price * oi.quantity) FROM OrderItem oi JOIN oi.order o JOIN oi.product p " +
           "WHERE o.status != 'CANCELLED' AND o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "GROUP BY p.brand ORDER BY SUM(oi.price * oi.quantity) DESC")
    List<Object[]> sumRevenueByBrandInPeriod(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query(value = "SELECT DATE(created_at) as date, SUM(total_price) as revenue " +
                   "FROM orders WHERE created_at >= :startDate AND created_at <= :endDate AND status != 'CANCELLED' " +
                   "GROUP BY DATE(created_at) ORDER BY date ASC", nativeQuery = true)
    List<Object[]> getRevenueTrendInPeriod(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);
    @Query("SELECT oi.product.name, SUM(oi.quantity), SUM(oi.price * oi.quantity) " +
           "FROM OrderItem oi JOIN oi.order o " +
           "WHERE o.status != 'CANCELLED' AND o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "GROUP BY oi.product.id, oi.product.name " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> sumTopSellingProductsInPeriod(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate, org.springframework.data.domain.Pageable pageable);

    // --- CRM / CUSTOMER ANALYTICS QUERIES ---

    // AOV (Average Order Value)
    @Query("SELECT COALESCE(AVG(o.totalPrice), 0) FROM Order o WHERE o.status = 'DELIVERED'")
    BigDecimal calculateAverageOrderValue();

    // Purchase Freq distribution
    @Query("SELECT o.user.id, COUNT(o) FROM Order o WHERE o.status = 'DELIVERED' GROUP BY o.user.id")
    List<Object[]> countUserPurchaseFrequencies();

    // RFM Scatter data: user_id, username, freq(COUNT), monetary(SUM), recency(MAX date)
    @Query("SELECT o.user.username, COUNT(o), SUM(o.totalPrice), MAX(o.createdAt) " +
           "FROM Order o WHERE o.status = 'DELIVERED' " +
           "GROUP BY o.user.username")
    List<Object[]> getRFMData();

    // High return rate list
    @Query("SELECT o.user.username, COUNT(o) " +
           "FROM Order o WHERE o.status IN ('CANCELLED', 'REFUNDED') " +
           "GROUP BY o.user.username ORDER BY COUNT(o) DESC")
    List<Object[]> getHighReturnRateUsers(org.springframework.data.domain.Pageable pageable);

    // --- MARKETING & VOUCHER ANALYTICS ---

    @Query("SELECT o.couponCode, COUNT(o), SUM(o.totalPrice), SUM(o.totalDiscount) " +
           "FROM Order o WHERE o.status = 'DELIVERED' AND o.couponCode IS NOT NULL " +
           "GROUP BY o.couponCode")
    List<Object[]> getVoucherPerformanceStats();

    @Query("SELECT o.shippingCouponCode, COUNT(o), SUM(o.shippingDiscount) " +
           "FROM Order o WHERE o.status = 'DELIVERED' AND o.shippingCouponCode IS NOT NULL " +
           "GROUP BY o.shippingCouponCode")
    List<Object[]> getShippingVoucherPerformanceStats();

    @Query("SELECT o.paymentMethod, COUNT(o), SUM(o.totalDiscount) " +
           "FROM Order o WHERE o.status = 'DELIVERED' AND o.couponCode IS NOT NULL " +
           "GROUP BY o.paymentMethod")
    List<Object[]> getVoucherUsageByPaymentMethod();

    @Query("SELECT COALESCE(AVG(o.totalPrice), 0) FROM Order o WHERE o.status = 'DELIVERED' AND o.couponCode IS NOT NULL")
    BigDecimal calculateAovWithVoucher();

    @Query("SELECT COALESCE(AVG(o.totalPrice), 0) FROM Order o WHERE o.status = 'DELIVERED' AND o.couponCode IS NULL")
    BigDecimal calculateAovWithoutVoucher();

    @Query(value = "SELECT HOUR(created_at) as hr, COUNT(*) as cnt FROM orders " +
                   "WHERE status = 'DELIVERED' AND (coupon_code IS NOT NULL OR shipping_coupon_code IS NOT NULL) " +
                   "GROUP BY hr ORDER BY hr ASC", nativeQuery = true)
    List<Object[]> getVoucherUsagePeakHours();

    // Retention: Users who used a voucher and then had another order (with or without voucher)
    @Query("SELECT COUNT(DISTINCT o1.user.id) FROM Order o1 " +
           "WHERE o1.couponCode IS NOT NULL AND EXISTS (" +
           "  SELECT 1 FROM Order o2 WHERE o2.user.id = o1.user.id AND o2.id > o1.id AND o2.status = 'DELIVERED'" +
           ")")
    long countRetainedVoucherUsers();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status != 'CANCELLED'")
    long countTotalValidOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status != 'CANCELLED' AND (o.couponCode IS NOT NULL OR o.shippingCouponCode IS NOT NULL)")
    long countOrdersWithAnyVoucher();
}