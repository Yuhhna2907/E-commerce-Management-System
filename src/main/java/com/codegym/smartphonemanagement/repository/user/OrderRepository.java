package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.Order;
import com.codegym.smartphonemanagement.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 1. Tìm lịch sử đơn hàng của 1 user
    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    // 2. Tìm đơn hàng theo trạng thái (Ví dụ: Admin muốn lọc các đơn PENDING)
    List<Order> findAllByStatusOrderByCreatedAtDesc(OrderStatus status);

    // 3. Tìm đơn hàng theo số điện thoại người nhận (Admin dùng để tra cứu nhanh)
    List<Order> findAllByReceiverPhoneContaining(String phone);

    // 1. Tính tổng doanh thu (Chỉ tính đơn đã thanh toán hoặc đang giao, bỏ qua đơn hủy)
    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.status != 'CANCELLED'")
    BigDecimal calculateTotalRevenue();

    // 2. Đếm tổng số đơn hàng thành công (Trạng thái SHIPPED)
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'SHIPPED'")
    long countCompletedOrders();

    // 3. Thống kê doanh thu 7 ngày gần nhất (Dùng Native Query cho MySQL)
    // Lưu ý: Duy kiểm tra tên cột ngày tạo trong DB là created_at đúng không nhé
    @Query(value = "SELECT DATE(created_at) as date, SUM(total_price) as revenue " +
            "FROM orders WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "AND status != 'CANCELLED' " +
            "GROUP BY DATE(created_at) ORDER BY date ASC", nativeQuery = true)
    List<Object[]> getRevenueLast7Days();

    // 4. Top 5 sản phẩm bán chạy nhất
    // Sử dụng JPQL để truy vấn qua OrderItem và Product
    @Query("SELECT oi.product.name, SUM(oi.quantity) as totalSold " +
            "FROM OrderItem oi " +
            "JOIN oi.order o " +
            "WHERE o.status != 'CANCELLED' " +
            "GROUP BY oi.product.id, oi.product.name " +
            "ORDER BY totalSold DESC")
    List<Object[]> getTopSellingProducts(org.springframework.data.domain.Pageable pageable);

    // 5. Thống kê số lượng đơn hàng theo từng trạng thái (Để vẽ biểu đồ tròn)
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();
}