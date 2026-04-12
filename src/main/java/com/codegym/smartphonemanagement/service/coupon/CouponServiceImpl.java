package com.codegym.smartphonemanagement.service.coupon;

import com.codegym.smartphonemanagement.model.*;
import com.codegym.smartphonemanagement.repository.CouponRepository;
import com.codegym.smartphonemanagement.repository.UserWalletRepository;
import com.codegym.smartphonemanagement.repository.user.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements ICouponService {

    private final CouponRepository couponRepository;
    private final UserWalletRepository userWalletRepository;
    private final OrderRepository orderRepository;

    @Override
    public List<Coupon> getAvailableCoupons() {
        return couponRepository.findByStatus(CouponStatus.ACTIVE).stream()
                .filter(c -> c.getEndDate().isAfter(LocalDateTime.now()))
                .filter(c -> c.getMaxUsageGlobal() == null || c.getCurrentUsageGlobal() < c.getMaxUsageGlobal())
                .collect(Collectors.toList());
    }

    @Override
    public List<Coupon> getUserWallet(User user) {
        return userWalletRepository.findByUser(user).stream()
                .map(UserWallet::getCoupon)
                .filter(c -> c.getStatus() == CouponStatus.ACTIVE && c.getEndDate().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveToWallet(User user, String code) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại"));

        if (userWalletRepository.existsByUserIdAndCouponId(user.getId(), coupon.getId())) {
            throw new RuntimeException("Bạn đã lưu mã này rồi");
        }

        UserWallet wallet = UserWallet.builder()
                .user(user)
                .coupon(coupon)
                .build();
        userWalletRepository.save(wallet);
    }

    @Override
    public CouponValidationResult validateCoupon(String code, User user, Order orderDraft) {
        if (code == null || code.trim().isEmpty()) {
            return CouponValidationResult.builder().valid(false).message("Mã không hợp lệ").build();
        }

        Coupon coupon = couponRepository.findByCode(code).orElse(null);
        if (coupon == null) {
            return CouponValidationResult.builder().valid(false).message("Mã không tồn tại").build();
        }
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            return CouponValidationResult.builder().valid(false).message("Mã này đang không hoạt động").build();
        }
        if (LocalDateTime.now().isAfter(coupon.getEndDate()) || LocalDateTime.now().isBefore(coupon.getStartDate())) {
            return CouponValidationResult.builder().valid(false).message("Mã đã hết hạn hoặc chưa đến thời gian lấy").build();
        }
        if (coupon.getMaxUsageGlobal() != null && coupon.getCurrentUsageGlobal() >= coupon.getMaxUsageGlobal()) {
            return CouponValidationResult.builder().valid(false).message("Mã đã hết lượt sử dụng").build();
        }

        // Per User Limit check (Optional basic logic checking order history)
        if (coupon.getPerUserLimit() != null && user != null) {
            long usedCount = orderRepository.findAll().stream()
                    .filter(o -> o.getUser() != null && o.getUser().getId().equals(user.getId()))
                    .filter(o -> code.equals(o.getCouponCode()))
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                    .count();
            if (usedCount >= coupon.getPerUserLimit()) {
                return CouponValidationResult.builder().valid(false).message("Bạn đã hết lượt sử dụng mã này").build();
            }
        }

        if (coupon.getMinOrderValue() != null && orderDraft.getTotalPrice().compareTo(coupon.getMinOrderValue()) < 0) {
            return CouponValidationResult.builder().valid(false)
                    .message("Đơn hàng chưa đạt giá trị tối thiểu " + coupon.getMinOrderValue() + "đ").build();
        }

        // Calculate discount
        BigDecimal discountAmt = calculateDiscountAmt(coupon, orderDraft);
        if (discountAmt.compareTo(BigDecimal.ZERO) == 0) {
            return CouponValidationResult.builder().valid(false).message("Sản phẩm trong giỏ không áp dụng được mã này").build();
        }

        return CouponValidationResult.builder()
                .valid(true)
                .message("Áp dụng mã thành công!")
                .discountAmount(discountAmt)
                .build();
    }

    private BigDecimal calculateDiscountAmt(Coupon coupon, Order order) {
        // Eligibility specific products check
        BigDecimal applicableTotal = BigDecimal.ZERO;
        boolean hasProductRestriction = coupon.getApplicableProducts() != null && !coupon.getApplicableProducts().isEmpty();

        for (OrderItem item : order.getItems()) {
            if (!hasProductRestriction || coupon.getApplicableProducts().contains(item.getProduct())) {
                BigDecimal itemTotal = item.getPrice().multiply(new BigDecimal(item.getQuantity()));
                applicableTotal = applicableTotal.add(itemTotal);
            }
        }

        if (applicableTotal.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        BigDecimal discount = BigDecimal.ZERO;
        if (coupon.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discount = coupon.getDiscountValue();
        } else {
            discount = applicableTotal.multiply(coupon.getDiscountValue().divide(new BigDecimal("100")));
            if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
        }

        if (discount.compareTo(applicableTotal) > 0) {
            discount = applicableTotal;
        }
        return discount;
    }

    @Override
    @Transactional
    public void applyDiscountToOrder(Order order) {
        if (order.getCouponCode() == null || order.getCouponCode().isEmpty()) {
            return;
        }

        Coupon coupon = couponRepository.findByCode(order.getCouponCode()).orElse(null);
        if (coupon == null) return;

        BigDecimal totalDiscount = calculateDiscountAmt(coupon, order);
        order.setTotalDiscount(totalDiscount);

        if (totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
            // Allocate discount among items proportionally
            BigDecimal applicableTotal = order.getItems().stream()
                    .filter(i -> coupon.getApplicableProducts() == null || coupon.getApplicableProducts().isEmpty() || coupon.getApplicableProducts().contains(i.getProduct()))
                    .map(i -> i.getPrice().multiply(new BigDecimal(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal allocatedSoFar = BigDecimal.ZERO;
            List<OrderItem> validItems = order.getItems().stream()
                    .filter(i -> coupon.getApplicableProducts() == null || coupon.getApplicableProducts().isEmpty() || coupon.getApplicableProducts().contains(i.getProduct()))
                    .collect(Collectors.toList());

            for (int i = 0; i < validItems.size(); i++) {
                OrderItem item = validItems.get(i);
                if (i == validItems.size() - 1) {
                    // Last item takes remainder to handle rounding issues
                    item.setAllocatedDiscount(totalDiscount.subtract(allocatedSoFar));
                } else {
                    BigDecimal itemTotal = item.getPrice().multiply(new BigDecimal(item.getQuantity()));
                    BigDecimal ratio = itemTotal.divide(applicableTotal, 4, RoundingMode.HALF_UP);
                    BigDecimal splitAlloc = totalDiscount.multiply(ratio).setScale(0, RoundingMode.HALF_UP);
                    item.setAllocatedDiscount(splitAlloc);
                    allocatedSoFar = allocatedSoFar.add(splitAlloc);
                }
            }
            
            // Tăng số lượng đã dùng với retry để handle race condition
            try {
                incrementCouponUsage(coupon);
            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                // Retry once if optimistic locking fails
                Coupon refreshedCoupon = couponRepository.findByCode(order.getCouponCode())
                        .orElseThrow(() -> new RuntimeException("Coupon không tồn tại"));
                incrementCouponUsage(refreshedCoupon);
            }
        }
    }

    private void incrementCouponUsage(Coupon coupon) {
        // Double-check usage limit before incrementing
        if (coupon.getMaxUsageGlobal() != null && coupon.getCurrentUsageGlobal() >= coupon.getMaxUsageGlobal()) {
            throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng");
        }
        coupon.setCurrentUsageGlobal(coupon.getCurrentUsageGlobal() + 1);
        couponRepository.save(coupon);
    }

    @Override
    @Transactional
    public void restoreVoucherUsage(String couponCode) {
        if (couponCode == null || couponCode.isEmpty()) return;
        Coupon coupon = couponRepository.findByCode(couponCode).orElse(null);
        if (coupon != null && coupon.getCurrentUsageGlobal() > 0) {
            coupon.setCurrentUsageGlobal(coupon.getCurrentUsageGlobal() - 1);
            couponRepository.save(coupon);
        }
    }
}
