package com.codegym.smartphonemanagement.service.dashboard.seller;

import com.codegym.smartphonemanagement.repository.seller.ProductRepository;
import com.codegym.smartphonemanagement.repository.user.*;
import com.codegym.smartphonemanagement.service.dashboard.DTO.DashboardDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final ProductQuestionRepository productQuestionRepository;
    private final com.codegym.smartphonemanagement.repository.CouponRepository couponRepository;
    private final com.codegym.smartphonemanagement.repository.SzWalletRepository szWalletRepository;
    private final com.codegym.smartphonemanagement.repository.SzWithdrawalRequestRepository szWithdrawalRequestRepository;

    public DashboardDTO getDashboardStats() {
        // 1. Lấy dữ liệu các con số tổng quát (Stat Cards cũ)
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue(); 
        BigDecimal actualRevenue = orderRepository.calculateActualRevenue(); 
        long completedOrders = orderRepository.countCompletedOrders();
        long shippingOrders = orderRepository.countShippingOrders(); 
        long totalProducts = productRepository.countByActiveTrue();
        long lowStockCount = productRepository.countByActiveTrueAndStockLessThan(5);

        // 1.2 LẤY DỮ LIỆU CÁC CON SỐ MỚI BỔ SUNG (KPI Mới)
        long totalStandardUsers = userRepository.countStandardUsers();
        BigDecimal totalPendingRefund = refundRequestRepository.calculateTotalPendingRefundAmount();
        long totalLoyaltyPoints = loyaltyAccountRepository.sumExtantPoints();
        long unansweredQuestions = productQuestionRepository.countAllUnansweredQuestions();
        long totalCoupons = couponRepository.count();
        BigDecimal totalWalletBalance = szWalletRepository.sumAllBalances();
        long pendingWithdrawalsCount = szWithdrawalRequestRepository.countByStatus(com.codegym.smartphonemanagement.model.WithdrawalStatus.PENDING);

        // 2. Lấy dữ liệu biểu đồ so sánh doanh thu 7 ngày
        List<Object[]> revenueComparisonData = orderRepository.getRevenueComparisonLast7Days();
        List<String> labels = new ArrayList<>();
        List<BigDecimal> actualValues = new ArrayList<>();
        List<BigDecimal> assumedValues = new ArrayList<>();

        for (Object[] row : revenueComparisonData) {
            labels.add(row[0].toString()); 
            actualValues.add(row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO); 
            assumedValues.add(row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO); 
        }

        // 3. Lấy dữ liệu Top 5 sản phẩm bán chạy
        List<Object[]> topProducts = orderRepository.getTopSellingProducts(PageRequest.of(0, 5));

        // 4. Lấy phân bổ phương thức thanh toán
        List<Object[]> paymentDistribution = orderRepository.getPaymentMethodDistribution();
        List<String> paymentLabels = new ArrayList<>();
        List<Long> paymentValues = new ArrayList<>();
        for (Object[] row : paymentDistribution) {
            paymentLabels.add(row[0] != null ? row[0].toString() : "Khác");
            paymentValues.add(((Number) row[1]).longValue());
        }

        // Đóng gói vào DashboardDTO
        return DashboardDTO.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .actualRevenue(actualRevenue != null ? actualRevenue : BigDecimal.ZERO)
                .totalOrders(completedOrders)
                .shippingOrders(shippingOrders)
                .totalProducts(totalProducts)
                .lowStockCount(lowStockCount)
                
                // DATA MỚI 
                .totalStandardUsers(totalStandardUsers)
                .totalPendingRefundAmount(totalPendingRefund != null ? totalPendingRefund : BigDecimal.ZERO)
                .totalLoyaltyPoints(totalLoyaltyPoints)
                .unansweredQuestions(unansweredQuestions)
                .totalCoupons(totalCoupons)
                .totalWalletBalance(totalWalletBalance != null ? totalWalletBalance : BigDecimal.ZERO)
                .pendingWithdrawalsCount(pendingWithdrawalsCount)
                
                .paymentMethodLabels(paymentLabels)
                .paymentMethodValues(paymentValues)

                // Biểu đồ doanh thu
                .revenueLabels(labels)
                .actualRevenueValues(actualValues)
                .assumedRevenueValues(assumedValues)
                .revenueValues(assumedValues) // Chống lỗi cũ

                // Biểu đồ Top sản phẩm
                .topProductNames(topProducts.stream()
                        .map(row -> row[0].toString()).toList())
                .topProductSales(topProducts.stream()
                        .map(row -> ((Number) row[1]).longValue()).toList())
                .build();
    }
}