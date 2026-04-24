package com.codegym.smartphonemanagement.service.dashboard.seller;

import com.codegym.smartphonemanagement.model.Coupon;
import com.codegym.smartphonemanagement.repository.CouponRepository;
import com.codegym.smartphonemanagement.repository.user.OrderRepository;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.repository.user.UserWalletRepository;
import com.codegym.smartphonemanagement.service.dashboard.DTO.VoucherAnalyticsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherAnalyticsService {

    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final UserWalletRepository userWalletRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "voucherAnalytics", key = "'allTime'")
    public VoucherAnalyticsDTO getVoucherAnalytics() {
        // 1. Basic Distributions
        List<Object[]> statusCounts = couponRepository.countByStatus();
        long active = 0, expired = 0, paused = 0, cancelled = 0;
        for (Object[] row : statusCounts) {
            String status = row[0].toString();
            long count = ((Number) row[1]).longValue();
            if (status.equals("ACTIVE")) active = count;
            else if (status.equals("EXPIRED")) expired = count;
            else if (status.equals("PAUSED")) paused = count;
            else if (status.equals("CANCELLED")) cancelled = count;
        }

        // 2. Performance Stats from Orders
        List<Object[]> perfRaw = orderRepository.getVoucherPerformanceStats();
        Map<String, VoucherAnalyticsDTO.VoucherMetric> metricMap = new HashMap<>();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        long totalUsage = 0;

        for (Object[] row : perfRaw) {
            String code = row[0] != null ? row[0].toString() : "UNKNOWN";
            long usage = ((Number) row[1]).longValue();
            BigDecimal revenue = new BigDecimal(row[2].toString());
            BigDecimal discount = new BigDecimal(row[3].toString());

            totalRevenue = totalRevenue.add(revenue);
            totalDiscount = totalDiscount.add(discount);
            totalUsage += usage;

            BigDecimal roi = discount.compareTo(BigDecimal.ZERO) > 0 
                ? revenue.subtract(discount).multiply(new BigDecimal(100)).divide(discount, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            metricMap.put(code, VoucherAnalyticsDTO.VoucherMetric.builder()
                    .code(code)
                    .usageCount(usage)
                    .totalRevenue(revenue)
                    .totalDiscount(discount)
                    .roi(roi)
                    .build());
        }

        // Add Shipping Vouchers
        List<Object[]> shipRaw = orderRepository.getShippingVoucherPerformanceStats();
        for (Object[] row : shipRaw) {
            String code = row[0] != null ? row[0].toString() : "UNKNOWN";
            long usage = ((Number) row[1]).longValue();
            BigDecimal shipDiscount = new BigDecimal(row[2].toString());

            VoucherAnalyticsDTO.VoucherMetric m = metricMap.getOrDefault(code, 
                VoucherAnalyticsDTO.VoucherMetric.builder().code(code).usageCount(0L).totalRevenue(BigDecimal.ZERO).totalDiscount(BigDecimal.ZERO).build());
            
            m.setUsageCount(m.getUsageCount() + usage);
            m.setTotalDiscount(m.getTotalDiscount().add(shipDiscount));
            metricMap.put(code, m);
            
            totalDiscount = totalDiscount.add(shipDiscount);
        }

        // 3. AOV Analytics
        BigDecimal aovWith = orderRepository.calculateAovWithVoucher();
        BigDecimal aovWithout = orderRepository.calculateAovWithoutVoucher();
        BigDecimal aovLift = aovWithout.compareTo(BigDecimal.ZERO) > 0 
            ? aovWith.subtract(aovWithout).multiply(new BigDecimal(100)).divide(aovWithout, 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Saturation
        long totalValidOrders = orderRepository.countTotalValidOrders();
        long ordersWithVoucher = orderRepository.countOrdersWithAnyVoucher();
        BigDecimal saturation = totalValidOrders > 0 
            ? new BigDecimal(ordersWithVoucher * 100).divide(new BigDecimal(totalValidOrders), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // 4. Time Analysis
        List<Object[]> peaks = orderRepository.getVoucherUsagePeakHours();
        List<VoucherAnalyticsDTO.HourStat> hourStats = peaks.stream()
                .map(r -> new VoucherAnalyticsDTO.HourStat(((Number) r[0]).intValue(), ((Number) r[1]).longValue(), BigDecimal.ZERO))
                .collect(Collectors.toList());

        // 5. Retention
        long retainedCount = orderRepository.countRetainedVoucherUsers();
        
        // 6. Enrichment and Funnel
        List<Coupon> allCoupons = couponRepository.findAll();
        List<VoucherAnalyticsDTO.VoucherMetric> fullMetrics = new ArrayList<>();
        long totalIssued = 0;
        
        for (Coupon c : allCoupons) {
            VoucherAnalyticsDTO.VoucherMetric m = metricMap.getOrDefault(c.getCode(), 
                VoucherAnalyticsDTO.VoucherMetric.builder()
                    .code(c.getCode())
                    .usageCount(0L)
                    .totalRevenue(BigDecimal.ZERO)
                    .totalDiscount(BigDecimal.ZERO)
                    .roi(BigDecimal.ZERO)
                    .build());
            
            m.setCouponId(c.getId());
            m.setDiscountType(c.getDiscountType().name());
            m.setCategory(c.getCouponCategory().getDescription());
            m.setDiscountDisplay(c.getDiscountType().name().equals("PERCENTAGE") ? c.getDiscountValue().intValue() + "%" : c.getDiscountValue().intValue() + "₫");
            m.setMaxUsage(c.getMaxUsageGlobal());
            m.setUsagePercent(c.getMaxUsageGlobal() != null && c.getMaxUsageGlobal() > 0 
                ? new BigDecimal(m.getUsageCount() * 100).divide(new BigDecimal(c.getMaxUsageGlobal()), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
            m.setStatus(c.getStatus().name());
            m.setStartDate(c.getStartDate().toString());
            m.setEndDate(c.getEndDate().toString());
            m.setDaysRemaining(ChronoUnit.DAYS.between(LocalDateTime.now(), c.getEndDate()));
            
            fullMetrics.add(m);
            if (c.getMaxUsageGlobal() != null) totalIssued += c.getMaxUsageGlobal();
        }

        long savedCount = userWalletRepository.countTotalSaved();
        
        // Affinity (Payment Methods)
        List<Object[]> payRaw = orderRepository.getVoucherUsageByPaymentMethod();
        List<VoucherAnalyticsDTO.CategoryStat> payStats = payRaw.stream().map(r -> 
            VoucherAnalyticsDTO.CategoryStat.builder()
                .label(r[0].toString())
                .count(((Number) r[1]).longValue())
                .totalDiscount(new BigDecimal(r[2].toString()))
                .build()
        ).collect(Collectors.toList());

        // 7. Alerts logic
        List<VoucherAnalyticsDTO.VoucherAlert> alerts = new ArrayList<>();
        fullMetrics.stream().filter(m -> m.getRoi().compareTo(BigDecimal.ZERO) < 0 && m.getUsageCount() > 5)
                .forEach(m -> alerts.add(VoucherAnalyticsDTO.VoucherAlert.builder()
                        .type("DANGER").icon("bi-exclamation-octagon").title("ROI Âm").message("Voucher " + m.getCode() + " đang có hiệu quả kinh tế âm!").couponCode(m.getCode()).build()));
        
        List<Coupon> expiring = couponRepository.findUpcomingExpiryCoupons(LocalDateTime.now().plusDays(3));
        expiring.forEach(c -> alerts.add(VoucherAnalyticsDTO.VoucherAlert.builder()
                .type("WARNING").icon("bi-clock-history").title("Sắp hết hạn").message("Voucher " + c.getCode() + " sẽ hết hạn trong vòng 3 ngày tới.").couponCode(c.getCode()).build()));

        fullMetrics.stream().filter(m -> m.getUsagePercent().compareTo(new BigDecimal(90)) > 0 && m.getMaxUsage() != null)
                .forEach(m -> alerts.add(VoucherAnalyticsDTO.VoucherAlert.builder()
                        .type("INFO").icon("bi-lightning-fill").title("Sắp cháy hàng").message("Voucher " + m.getCode() + " đã sử dụng hơn 90% ngân sách.").couponCode(m.getCode()).build()));

        return VoucherAnalyticsDTO.builder()
                .totalActiveVouchers(active)
                .totalExpiredVouchers(expired)
                .totalPausedVouchers(paused)
                .totalCancelledVouchers(cancelled)
                .totalUsageCount(totalUsage)
                .totalDiscountBurned(totalDiscount)
                .totalRevenueGenerated(totalRevenue)
                .overallROI(totalDiscount.compareTo(BigDecimal.ZERO) > 0 
                        ? totalRevenue.subtract(totalDiscount).multiply(new BigDecimal(100)).divide(totalDiscount, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .aovWithVoucher(aovWith)
                .aovWithoutVoucher(aovWithout)
                .aovLift(aovLift)
                .voucherSaturation(saturation)
                .funnelIssued(totalIssued)
                .funnelSaved(savedCount)
                .funnelApplied(ordersWithVoucher)
                .funnelCompleted(totalUsage)
                .byPaymentMethod(payStats)
                .topByUsage(fullMetrics.stream().sorted((a,b) -> b.getUsageCount().compareTo(a.getUsageCount())).limit(10).collect(Collectors.toList()))
                .topByRevenue(fullMetrics.stream().sorted((a,b) -> b.getTotalRevenue().compareTo(a.getTotalRevenue())).limit(10).collect(Collectors.toList()))
                .topByROI(fullMetrics.stream().sorted((a,b) -> b.getRoi().compareTo(a.getRoi())).limit(10).collect(Collectors.toList()))
                .usageByHour(hourStats)
                .retainedUsersCount(retainedCount)
                .alerts(alerts)
                .allVouchers(fullMetrics)
                .build();
    }
}
