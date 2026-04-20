package com.codegym.smartphonemanagement.service.coupon;

import com.codegym.smartphonemanagement.model.*;
import com.codegym.smartphonemanagement.repository.CouponRepository;
import com.codegym.smartphonemanagement.repository.user.UserWalletRepository;
import com.codegym.smartphonemanagement.repository.user.OrderRepository;
import com.codegym.smartphonemanagement.exception.EntityNotFoundException;
import com.codegym.smartphonemanagement.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements ICouponService {

    private final CouponRepository couponRepository;
    private final UserWalletRepository userWalletRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Coupon> getAvailableCoupons() {
        log.debug("Fetching available coupons");
        LocalDateTime now = LocalDateTime.now();
        
        return couponRepository.findByStatus(CouponStatus.ACTIVE).stream()
                .filter(c -> c.getEndDate().isAfter(now))
                .filter(c -> c.getMaxUsageGlobal() == null || c.getCurrentUsageGlobal() < c.getMaxUsageGlobal())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Coupon> getUserWallet(User user) {
        validateUser(user);
        log.debug("Fetching wallet for user ID: {}", user.getId());
        
        LocalDateTime now = LocalDateTime.now();
        return userWalletRepository.findByUser(user).stream()
                .map(UserWallet::getCoupon)
                .filter(c -> c.getStatus() == CouponStatus.ACTIVE && c.getEndDate().isAfter(now))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveToWallet(User user, String code) {
        validateUser(user);
        validateCouponCode(code);
        
        log.debug("Saving coupon '{}' to wallet for user ID: {}", code, user.getId());
        
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Mã giảm giá không tồn tại"));

        if (userWalletRepository.existsByUserIdAndCouponId(user.getId(), coupon.getId())) {
            log.warn("User {} already has coupon {} in wallet", user.getId(), code);
            throw new BadRequestException("Bạn đã lưu mã này rồi");
        }

        UserWallet wallet = UserWallet.builder()
                .user(user)
                .coupon(coupon)
                .build();
        userWalletRepository.save(wallet);
        
        log.info("Successfully saved coupon '{}' to wallet for user ID: {}", code, user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResult validateCoupon(String code, User user, Order orderDraft) {
        // Input validation
        if (code == null || code.trim().isEmpty()) {
            return buildInvalidResult("Mã không hợp lệ");
        }
        
        validateOrder(orderDraft);

        // Find coupon
        Coupon coupon = couponRepository.findByCode(code).orElse(null);
        if (coupon == null) {
            return buildInvalidResult("Mã không tồn tại");
        }
        
        // Validate coupon status and dates
        CouponValidationResult statusValidation = validateCouponStatus(coupon);
        if (!statusValidation.isValid()) {
            return statusValidation;
        }
        
        // Validate usage limits
        CouponValidationResult usageValidation = validateCouponUsage(coupon, user, code);
        if (!usageValidation.isValid()) {
            return usageValidation;
        }
        
        // Validate minimum order value
        if (coupon.getMinOrderValue() != null && 
            orderDraft.getTotalPrice().compareTo(coupon.getMinOrderValue()) < 0) {
            return buildInvalidResult("Đơn hàng chưa đạt giá trị tối thiểu " + 
                coupon.getMinOrderValue() + "đ");
        }

        // Validate payment methods
        if (coupon.getApplicablePaymentMethods() != null && !coupon.getApplicablePaymentMethods().isEmpty()) {
            if (orderDraft.getPaymentMethod() != null && !coupon.getApplicablePaymentMethods().contains(orderDraft.getPaymentMethod())) {
                return buildInvalidResult("Mã này chỉ áp dụng cho một số phương thức thanh toán nhất định");
            }
        }

        // Calculate discount
        BigDecimal discountAmt = calculateDiscountAmount(coupon, orderDraft);
        if (discountAmt.compareTo(BigDecimal.ZERO) == 0) {
            return buildInvalidResult("Sản phẩm trong giỏ không áp dụng được mã này");
        }

        log.info("Coupon '{}' validated successfully with discount: {}", code, discountAmt);
        return CouponValidationResult.builder()
                .valid(true)
                .message("Áp dụng mã thành công!")
                .discountAmount(discountAmt)
                .build();
    }
    
    /**
     * Validate coupon status and dates
     */
    private CouponValidationResult validateCouponStatus(Coupon coupon) {
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            return buildInvalidResult("Mã này đang không hoạt động");
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(coupon.getEndDate())) {
            return buildInvalidResult("Mã đã hết hạn");
        }
        
        if (now.isBefore(coupon.getStartDate())) {
            return buildInvalidResult("Mã chưa đến thời gian sử dụng");
        }
        
        return CouponValidationResult.builder().valid(true).build();
    }
    
    /**
     * Validate coupon usage limits (global and per-user)
     */
    private CouponValidationResult validateCouponUsage(Coupon coupon, User user, String code) {
        // Check global usage limit
        if (coupon.getMaxUsageGlobal() != null && 
            coupon.getCurrentUsageGlobal() >= coupon.getMaxUsageGlobal()) {
            return buildInvalidResult("Mã đã hết lượt sử dụng");
        }

        // Check per-user limit
        if (coupon.getPerUserLimit() != null && user != null) {
            long usedCount = orderRepository.findAll().stream()
                    .filter(o -> o.getUser() != null && o.getUser().getId().equals(user.getId()))
                    .filter(o -> code.equals(o.getCouponCode()))
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                    .count();
            
            if (usedCount >= coupon.getPerUserLimit()) {
                return buildInvalidResult("Bạn đã hết lượt sử dụng mã này");
            }
        }
        
        return CouponValidationResult.builder().valid(true).build();
    }
    
    /**
     * Build invalid validation result
     */
    private CouponValidationResult buildInvalidResult(String message) {
        return CouponValidationResult.builder()
                .valid(false)
                .message(message)
                .build();
    }

    /**
     * Calculate discount amount based on coupon type and order items
     */
    private BigDecimal calculateDiscountAmount(Coupon coupon, Order order) {
        BigDecimal applicableTotal = calculateApplicableTotal(coupon, order);
        
        if (applicableTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = calculateRawDiscount(coupon, applicableTotal);
        
        // Cap discount at applicable total
        if (discount.compareTo(applicableTotal) > 0) {
            discount = applicableTotal;
        }
        
        return discount;
    }
    
    /**
     * Calculate total amount eligible for discount
     */
    private BigDecimal calculateApplicableTotal(Coupon coupon, Order order) {
        if (coupon.getCouponCategory() == CouponCategory.ORDER_DISCOUNT) {
            return order.getTotalPrice();
        } else if (coupon.getCouponCategory() == CouponCategory.FREE_SHIPPING) {
            return order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        }

        BigDecimal applicableTotal = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            if (isItemApplicable(item, coupon)) {
                BigDecimal itemTotal = item.getPrice().multiply(new BigDecimal(item.getQuantity()));
                applicableTotal = applicableTotal.add(itemTotal);
            }
        }
        
        return applicableTotal;
    }
    
    /**
     * Check if order item is applicable for discount
     */
    private boolean isItemApplicable(OrderItem item, Coupon coupon) {
        Product product = item.getProduct();
        if (product == null) return false;

        boolean hasProductRestriction = coupon.getApplicableProducts() != null && !coupon.getApplicableProducts().isEmpty();
        boolean hasCategoryRestriction = coupon.getApplicableCategories() != null && !coupon.getApplicableCategories().isEmpty();
        boolean hasBrandRestriction = coupon.getApplicableBrands() != null && !coupon.getApplicableBrands().isEmpty();

        if (!hasProductRestriction && !hasCategoryRestriction && !hasBrandRestriction) {
            return true;
        }

        boolean match = false;
        if (hasProductRestriction && coupon.getApplicableProducts().stream().anyMatch(p -> p.getId().equals(product.getId()))) {
            match = true;
        }
        if (!match && hasCategoryRestriction && product.getCategory() != null && coupon.getApplicableCategories().stream().anyMatch(c -> c.getId().equals(product.getCategory().getId()))) {
            match = true;
        }
        if (!match && hasBrandRestriction && product.getBrand() != null && coupon.getApplicableBrands().contains(product.getBrand())) {
            match = true;
        }

        return match;
    }
    
    /**
     * Calculate raw discount based on coupon type
     */
    private BigDecimal calculateRawDiscount(Coupon coupon, BigDecimal applicableTotal) {
        BigDecimal discount;
        
        if (coupon.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discount = coupon.getDiscountValue();
        } else {
            // PERCENTAGE type
            discount = applicableTotal
                    .multiply(coupon.getDiscountValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    .setScale(0, RoundingMode.DOWN);
            
            // Apply max discount cap if set
            if (coupon.getMaxDiscountAmount() != null && 
                discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
        }
        
        return discount;
    }

    @Override
    @Transactional
    public void applyDiscountToOrder(Order order) {
        // Áp dụng Shipping Coupon
        if (order.getShippingCouponCode() != null && !order.getShippingCouponCode().isEmpty()) {
            Coupon shipCoupon = couponRepository.findByCodeWithProducts(order.getShippingCouponCode()).orElse(null);
            if (shipCoupon != null && shipCoupon.getCouponCategory() == CouponCategory.FREE_SHIPPING) {
                BigDecimal shipDiscount = calculateDiscountAmount(shipCoupon, order);
                order.setShippingDiscount(shipDiscount);
                incrementCouponUsageWithRetry(shipCoupon, order.getShippingCouponCode());
            }
        }

        // Áp dụng Order Coupon
        if (order.getCouponCode() == null || order.getCouponCode().isEmpty()) {
            return;
        }

        log.debug("Applying coupon '{}' to order ID: {}", order.getCouponCode(), order.getId());
        
        Coupon coupon = couponRepository.findByCodeWithProducts(order.getCouponCode())
                .orElseThrow(() -> new EntityNotFoundException("Coupon không tồn tại"));

        BigDecimal totalDiscount = calculateDiscountAmount(coupon, order);
        order.setTotalDiscount(totalDiscount);

        if (totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
            if (coupon.getCouponCategory() == CouponCategory.PRODUCT_DISCOUNT) {
                allocateDiscountToItems(coupon, order, totalDiscount);
            }
            incrementCouponUsageWithRetry(coupon, order.getCouponCode());
            
            log.info("Successfully applied coupon '{}' with discount {} to order ID: {}", 
                    order.getCouponCode(), totalDiscount, order.getId());
        }
    }
    
    private void allocateDiscountToItems(Coupon coupon, Order order, BigDecimal totalDiscount) {
        List<OrderItem> validItems = order.getItems().stream()
                .filter(i -> isItemApplicable(i, coupon))
                .collect(Collectors.toList());
        
        if (validItems.isEmpty()) {
            return;
        }
        
        BigDecimal applicableTotal = validItems.stream()
                .map(i -> i.getPrice().multiply(new BigDecimal(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal allocatedSoFar = BigDecimal.ZERO;
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
    }
    
    /**
     * Increment coupon usage with retry on optimistic locking failure
     */
    @Retryable(
        value = org.springframework.orm.ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    private void incrementCouponUsageWithRetry(Coupon coupon, String couponCode) {
        try {
            incrementCouponUsage(coupon);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic locking failure for coupon '{}', retrying...", couponCode);
            // Refresh coupon and retry
            Coupon refreshedCoupon = couponRepository.findByCode(couponCode)
                    .orElseThrow(() -> new EntityNotFoundException("Coupon không tồn tại"));
            incrementCouponUsage(refreshedCoupon);
        }
    }

    /**
     * Increment coupon usage count with validation
     */
    private void incrementCouponUsage(Coupon coupon) {
        // Double-check usage limit before incrementing
        if (coupon.getMaxUsageGlobal() != null && 
            coupon.getCurrentUsageGlobal() >= coupon.getMaxUsageGlobal()) {
            throw new BadRequestException("Mã giảm giá đã hết lượt sử dụng");
        }
        
        coupon.setCurrentUsageGlobal(coupon.getCurrentUsageGlobal() + 1);
        couponRepository.save(coupon);
        
        log.debug("Incremented usage count for coupon ID: {} to {}", 
                coupon.getId(), coupon.getCurrentUsageGlobal());
    }

    @Override
    @Transactional
    public void restoreVoucherUsage(String couponCode) {
        validateCouponCode(couponCode);
        
        log.debug("Restoring voucher usage for coupon: {}", couponCode);
        
        Coupon coupon = couponRepository.findByCode(couponCode)
                .orElseThrow(() -> new EntityNotFoundException("Coupon không tồn tại"));
        
        if (coupon.getCurrentUsageGlobal() > 0) {
            coupon.setCurrentUsageGlobal(coupon.getCurrentUsageGlobal() - 1);
            couponRepository.save(coupon);
            
            log.info("Restored voucher usage for coupon '{}', new count: {}", 
                    couponCode, coupon.getCurrentUsageGlobal());
        }
    }

    /**
     * Get set of product IDs that are applicable for this coupon
     */
    private Set<Long> getApplicableProductIds(Coupon coupon) {
        if (coupon.getApplicableProducts() == null || coupon.getApplicableProducts().isEmpty()) {
            return Set.of();
        }
        return coupon.getApplicableProducts().stream()
                .map(Product::getId)
                .collect(Collectors.toSet());
    }
    
    // ==================== Validation Helper Methods ====================
    
    /**
     * Validate user is not null
     */
    private void validateUser(User user) {
        if (user == null) {
            throw new BadRequestException("User không được null");
        }
        if (user.getId() == null || user.getId() <= 0) {
            throw new BadRequestException("User ID không hợp lệ");
        }
    }
    
    /**
     * Validate coupon code is not empty
     */
    private void validateCouponCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new BadRequestException("Mã coupon không được rỗng");
        }
    }
    
    /**
     * Validate order has items
     */
    private void validateOrder(Order order) {
        if (order == null) {
            throw new BadRequestException("Order không được null");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new BadRequestException("Order phải có ít nhất 1 sản phẩm");
        }
    }

    /**
     * Scheduled task to check and expire coupons
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 * * * * *") // Run every minute
    @Transactional
    public void autoExpireCoupons() {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> activeCoupons = couponRepository.findByStatus(CouponStatus.ACTIVE);
        
        int expiredCount = 0;
        for (Coupon coupon : activeCoupons) {
            boolean shouldExpire = false;
            
            // Reached usage limit
            if (coupon.getMaxUsageGlobal() != null && coupon.getCurrentUsageGlobal() >= coupon.getMaxUsageGlobal()) {
                shouldExpire = true;
            }
            
            // Passed end date
            if (coupon.getEndDate() != null && coupon.getEndDate().isBefore(now)) {
                shouldExpire = true;
            }
            
            if (shouldExpire) {
                coupon.setStatus(CouponStatus.EXPIRED);
                couponRepository.save(coupon);
                expiredCount++;
            }
        }
        
        if (expiredCount > 0) {
            log.info("Auto-expired {} coupons", expiredCount);
        }
    }
}
