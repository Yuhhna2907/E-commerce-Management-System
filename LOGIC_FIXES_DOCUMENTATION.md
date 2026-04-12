# Logic Fixes Documentation - OMS & Refund System

## Overview
This document details all logic errors identified and fixed in the Order Management System (OMS) and Refund System.

**Date**: April 12, 2026  
**Status**: ✅ All fixes implemented  
**Files Modified**: 4 core service files + 1 model file

---

## Summary of Fixes

| # | Issue | Severity | Status |
|---|-------|----------|--------|
| 1 | Refund duplicate validation missing | 🔴 Critical | ✅ Fixed |
| 2 | Refund amount validation missing | 🔴 Critical | ✅ Fixed |
| 3 | Refund window validation incomplete | 🟡 Medium | ✅ Fixed |
| 4 | Partial refund totalPrice not updated | 🔴 Critical | ✅ Fixed |
| 5 | Voucher double restoration possible | 🔴 Critical | ✅ Fixed |
| 6 | State machine CANCELLED transition missing | 🟡 Medium | ✅ Fixed |
| 7 | canRefund fallback logic unsafe | 🟡 Medium | ✅ Fixed |
| 8 | Race condition in voucher usage | 🔴 Critical | ✅ Fixed |

---

## Detailed Fixes

### Fix #1: Refund Duplicate Validation Missing
**File**: `RefundService.java`  
**Location**: `createRefundRequest()` method  
**Severity**: 🔴 Critical

#### Problem
Users could request refunds for the same items multiple times, potentially getting refunds exceeding the original purchase quantity.

#### Example Attack Scenario
```
Order: 5 units of Product A
Refund Request 1: 3 units → Approved
Refund Request 2: 3 units → Would be approved (WRONG!)
Total refunded: 6 units > 5 units purchased
```

#### Solution
Added validation to check already refunded quantities:

```java
// Calculate already refunded quantity for this item
int alreadyRefundedQty = refundRequestRepository.findByOrderId(order.getId())
        .stream()
        .filter(r -> r.getStatus() == RefundStatus.APPROVED)
        .flatMap(r -> r.getItems().stream())
        .filter(ri -> ri.getOrderItem().getId().equals(orderItem.getId()))
        .mapToInt(RefundItem::getQuantity)
        .sum();

// Validate quantity: current + already refunded <= original quantity
int remainingQty = orderItem.getQuantity() - alreadyRefundedQty;
if (itemDto.getQuantity() > remainingQty) {
    throw new RuntimeException("Số lượng trả lại không hợp lệ cho " + orderItem.getProduct().getName() 
            + ". Đã mua: " + orderItem.getQuantity() 
            + ", Đã hoàn trả: " + alreadyRefundedQty 
            + ", Còn lại: " + remainingQty);
}
```

#### Impact
- ✅ Prevents fraud/abuse
- ✅ Ensures refund quantity never exceeds purchased quantity
- ✅ Supports partial refunds correctly

---

### Fix #2: Refund Amount Validation Missing
**File**: `RefundService.java`  
**Location**: `createRefundRequest()` method  
**Severity**: 🔴 Critical

#### Problem
If a product was discounted 100% (free), the refund amount would be 0 or negative, causing accounting errors.

#### Example Scenario
```
Product: 100,000đ
Discount: 100% (100,000đ)
Net price: 0đ
Refund amount: 0đ → Should not allow refund
```

#### Solution
Added validation to ensure refund amount > 0:

```java
// Validate refund amount > 0
if (itemRefund.compareTo(BigDecimal.ZERO) <= 0) {
    throw new RuntimeException("Số tiền hoàn trả không hợp lệ cho " + orderItem.getProduct().getName() 
            + ". Sản phẩm này có thể đã được giảm giá 100%.");
}
```

#### Impact
- ✅ Prevents refunds for free items
- ✅ Protects against negative refund amounts
- ✅ Clear error message for users

---

### Fix #3: Refund Window Validation Incomplete
**File**: `RefundService.java`  
**Location**: `createRefundRequest()` method  
**Severity**: 🟡 Medium

#### Problem
If no DELIVERED event was found in order history, the code would silently allow refunds (unsafe fallback).

#### Original Code
```java
Optional<OrderHistory> deliveredEvent = ...findFirst();
LocalDateTime deliveredAt = deliveredEvent.isPresent() 
    ? deliveredEvent.get().getCreatedAt() 
    : LocalDateTime.now(); // UNSAFE FALLBACK
```

#### Solution
Changed to throw exception if DELIVERED event not found:

```java
if (!deliveredEvent.isPresent()) {
    throw new RuntimeException("Không tìm thấy thông tin giao hàng. Vui lòng liên hệ hỗ trợ.");
}

LocalDateTime deliveredAt = deliveredEvent.get().getCreatedAt();
if (deliveredAt.plusDays(REFUND_WINDOW_DAYS).isBefore(LocalDateTime.now())) {
    throw new RuntimeException("Đã quá thời hạn " + REFUND_WINDOW_DAYS + " ngày để yêu cầu hoàn trả.");
}
```

#### Impact
- ✅ Safer validation - no silent fallbacks
- ✅ Forces investigation if order history is corrupted
- ✅ Prevents refunds when delivery date is unknown

---

### Fix #4: Partial Refund totalPrice Not Updated
**File**: `RefundService.java`  
**Location**: `approveRefund()` method  
**Severity**: 🔴 Critical

#### Problem
When approving a refund, the order's `totalPrice` was not updated, causing accounting discrepancies.

#### Example Scenario
```
Original order: 1,000,000đ
Refund: 300,000đ
Order totalPrice: Still 1,000,000đ (WRONG!)
Should be: 700,000đ
```

#### Solution
Added logic to subtract refund amount from order totalPrice:

```java
// Cập nhật totalPrice của order (trừ đi số tiền refund)
BigDecimal currentTotal = order.getTotalPrice();
BigDecimal newTotal = currentTotal.subtract(req.getTotalRefundAmount());
if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
    newTotal = BigDecimal.ZERO;
}
order.setTotalPrice(newTotal);
order.setStatus(newStatus);
orderRepository.save(order);
```

#### Impact
- ✅ Accurate order totals after refunds
- ✅ Correct financial reporting
- ✅ Prevents negative totals with safety check

---

### Fix #5: Voucher Double Restoration Possible
**File**: `RefundService.java`  
**Location**: `approveRefund()` method  
**Severity**: 🔴 Critical

#### Problem
If multiple refunds were approved for the same order, the voucher could be restored multiple times.

#### Example Attack Scenario
```
Order uses voucher "SAVE20" (1 usage)
Partial refund 1 → Voucher restored (usage = 0)
Partial refund 2 → Voucher restored again (usage = -1) ← WRONG!
```

#### Solution
Added history check to prevent double restoration:

```java
if (allItemsFullyRefunded) {
    newStatus = OrderStatus.REFUNDED;
    // Chỉ hoàn voucher nếu chưa từng hoàn trước đó
    if (order.getCouponCode() != null && !order.getCouponCode().isEmpty()) {
        // Check xem đã hoàn voucher chưa bằng cách kiểm tra history
        boolean voucherAlreadyRestored = orderHistoryRepository
                .findByOrderIdOrderByCreatedAtDesc(order.getId())
                .stream()
                .anyMatch(h -> h.getStatusTo() == OrderStatus.REFUNDED 
                        && h.getReason() != null 
                        && h.getReason().contains("hoàn voucher"));
        
        if (!voucherAlreadyRestored) {
            shouldRestoreVoucher = true;
        }
    }
}
```

#### Impact
- ✅ Prevents voucher usage from going negative
- ✅ Ensures voucher restored only once per order
- ✅ Maintains voucher usage integrity

---

### Fix #6: State Machine CANCELLED Transition Missing
**File**: `OrderService.java`  
**Location**: `VALID_TRANSITIONS` map  
**Severity**: 🟡 Medium

#### Problem
Terminal states (CANCELLED, REFUNDED, PARTIAL_REFUNDED) were not properly defined, allowing invalid transitions.

#### Original Code
```java
private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
    OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
    OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED),
    OrderStatus.SHIPPING, EnumSet.of(OrderStatus.DELIVERED),
    OrderStatus.DELIVERED, EnumSet.of(OrderStatus.REFUND_REQUESTED),
    OrderStatus.REFUND_REQUESTED, EnumSet.of(OrderStatus.REFUNDED, OrderStatus.PARTIAL_REFUNDED, OrderStatus.DELIVERED)
    // CANCELLED, REFUNDED, PARTIAL_REFUNDED missing!
);
```

#### Solution
Added terminal states and proper transitions:

```java
private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
    OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
    OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED),
    OrderStatus.SHIPPING, EnumSet.of(OrderStatus.DELIVERED),
    OrderStatus.DELIVERED, EnumSet.of(OrderStatus.REFUND_REQUESTED),
    OrderStatus.REFUND_REQUESTED, EnumSet.of(OrderStatus.REFUNDED, OrderStatus.PARTIAL_REFUNDED, OrderStatus.DELIVERED),
    OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class), // Terminal state
    OrderStatus.REFUNDED, EnumSet.noneOf(OrderStatus.class), // Terminal state
    OrderStatus.PARTIAL_REFUNDED, EnumSet.of(OrderStatus.REFUND_REQUESTED, OrderStatus.REFUNDED) // Allow additional refunds
);
```

#### Impact
- ✅ Prevents invalid state transitions
- ✅ Properly defines terminal states
- ✅ Allows multiple partial refunds correctly

---

### Fix #7: canRefund Fallback Logic Unsafe
**File**: `OrderService.java`  
**Location**: `mapToResponseDTO()` method  
**Severity**: 🟡 Medium

#### Problem
If no DELIVERED event was found, the code would default to `canRefund = true` (unsafe).

#### Original Code
```java
boolean canRefund = false;
if (order.getStatus() == OrderStatus.DELIVERED) {
    Optional<OrderHistory> deliveredEvent = ...findFirst();
    if (deliveredEvent.isPresent()) {
        LocalDateTime deliveredAt = deliveredEvent.get().getCreatedAt();
        canRefund = deliveredAt.plusDays(14).isAfter(LocalDateTime.now());
    } else {
        canRefund = true; // UNSAFE FALLBACK
    }
}
```

#### Solution
Removed unsafe fallback - if no event found, canRefund stays false:

```java
boolean canRefund = false;
if (order.getStatus() == OrderStatus.DELIVERED) {
    Optional<OrderHistory> deliveredEvent = orderHistoryRepository
            .findByOrderIdOrderByCreatedAtDesc(order.getId())
            .stream()
            .filter(h -> h.getStatusTo() == OrderStatus.DELIVERED)
            .findFirst();
    if (deliveredEvent.isPresent()) {
        LocalDateTime deliveredAt = deliveredEvent.get().getCreatedAt();
        canRefund = deliveredAt.plusDays(14).isAfter(LocalDateTime.now());
    }
    // Nếu không tìm thấy event, không cho refund (an toàn hơn)
}
```

#### Impact
- ✅ Safer default behavior
- ✅ Prevents refunds when delivery date is unknown
- ✅ Forces investigation of data integrity issues

---

### Fix #8: Race Condition in Voucher Usage
**File**: `CouponServiceImpl.java` + `Coupon.java`  
**Location**: `applyDiscountToOrder()` method + entity model  
**Severity**: 🔴 Critical

#### Problem
Multiple users could use the same voucher simultaneously, exceeding `maxUsageGlobal` limit.

#### Example Race Condition
```
Voucher "SAVE50": maxUsageGlobal = 100, currentUsageGlobal = 99

Time    User A                          User B
----    ------                          ------
T1      Read: currentUsageGlobal = 99   
T2                                      Read: currentUsageGlobal = 99
T3      Check: 99 < 100 ✓               
T4                                      Check: 99 < 100 ✓
T5      Write: currentUsageGlobal = 100 
T6                                      Write: currentUsageGlobal = 100
Result: Both succeed, but should be 101 (exceeded limit!)
```

#### Solution
Implemented optimistic locking with `@Version` annotation:

**Coupon.java**:
```java
@Entity
@Table(name = "coupons")
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version")
    private Long version; // For optimistic locking
    
    // ... other fields
}
```

**CouponServiceImpl.java**:
```java
// Tăng số lượng đã dùng với retry để handle race condition
try {
    incrementCouponUsage(coupon);
} catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
    // Retry once if optimistic locking fails
    Coupon refreshedCoupon = couponRepository.findByCode(order.getCouponCode())
            .orElseThrow(() -> new RuntimeException("Coupon không tồn tại"));
    incrementCouponUsage(refreshedCoupon);
}

private void incrementCouponUsage(Coupon coupon) {
    // Double-check usage limit before incrementing
    if (coupon.getMaxUsageGlobal() != null && coupon.getCurrentUsageGlobal() >= coupon.getMaxUsageGlobal()) {
        throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng");
    }
    coupon.setCurrentUsageGlobal(coupon.getCurrentUsageGlobal() + 1);
    couponRepository.save(coupon);
}
```

#### How Optimistic Locking Works
1. When reading a Coupon, JPA also reads the `version` field
2. When saving, JPA checks if `version` in DB matches the read version
3. If versions match → Update succeeds, version incremented
4. If versions don't match → `ObjectOptimisticLockingFailureException` thrown
5. Retry logic refreshes the coupon and tries again

#### Impact
- ✅ Prevents race conditions in concurrent voucher usage
- ✅ Ensures maxUsageGlobal limit is never exceeded
- ✅ Automatic retry on conflict (user-friendly)

#### Database Migration Required
Run this SQL script before deploying:
```sql
-- See: add-version-to-coupons.sql
ALTER TABLE coupons ADD COLUMN version BIGINT DEFAULT 0;
UPDATE coupons SET version = 0 WHERE version IS NULL;
ALTER TABLE coupons MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;
```

---

## Testing Checklist

### Before Testing
- [ ] Run `fix-shipped-to-shipping.sql` to fix enum data
- [ ] Run `add-version-to-coupons.sql` to add version column
- [ ] Restart application to load new code

### Test Cases

#### Test #1: Refund Duplicate Prevention
1. Create order with 5 units of Product A
2. Mark order as DELIVERED
3. Request refund for 3 units → Should succeed
4. Admin approves refund
5. Request refund for 3 units again → Should fail with clear error message
6. Request refund for 2 units → Should succeed (remaining quantity)

#### Test #2: Refund Amount Validation
1. Create order with product that has 100% discount (free item)
2. Mark order as DELIVERED
3. Request refund → Should fail with "giảm giá 100%" error

#### Test #3: Refund Window Validation
1. Create order and mark as DELIVERED
2. Manually delete DELIVERED event from order_history table
3. Request refund → Should fail with "Không tìm thấy thông tin giao hàng" error

#### Test #4: Partial Refund totalPrice Update
1. Create order with totalPrice = 1,000,000đ
2. Mark as DELIVERED
3. Request partial refund for 300,000đ
4. Admin approves
5. Check order.totalPrice → Should be 700,000đ

#### Test #5: Voucher Double Restoration Prevention
1. Create order with voucher "SAVE20"
2. Check voucher currentUsageGlobal (should be +1)
3. Mark order as DELIVERED
4. Request partial refund → Admin approves
5. Request another partial refund for remaining items → Admin approves
6. Check voucher currentUsageGlobal → Should be -1 only once, not -2

#### Test #6: State Machine Terminal States
1. Create order and mark as CANCELLED
2. Try to change status to CONFIRMED → Should fail
3. Create order, mark as DELIVERED, full refund
4. Try to change status from REFUNDED → Should fail

#### Test #7: canRefund Fallback Safety
1. Create order and mark as DELIVERED
2. Manually delete DELIVERED event from order_history
3. View order detail page
4. Check "Yêu cầu hoàn trả" button → Should be disabled

#### Test #8: Race Condition Prevention
1. Create voucher with maxUsageGlobal = 100, currentUsageGlobal = 99
2. Simulate 2 concurrent orders using the same voucher
3. One should succeed (usage = 100), one should fail with "hết lượt sử dụng"
4. Check currentUsageGlobal → Should be exactly 100, not 101

---

## Files Modified

| File | Lines Changed | Description |
|------|---------------|-------------|
| `RefundService.java` | ~50 lines | Fixes #1, #2, #3, #4, #5 |
| `OrderService.java` | ~15 lines | Fixes #6, #7 |
| `CouponServiceImpl.java` | ~20 lines | Fix #8 |
| `Coupon.java` | +3 lines | Fix #8 (added @Version) |

---

## Database Migrations Required

### Migration 1: Fix SHIPPED → SHIPPING
**File**: `fix-shipped-to-shipping.sql`  
**Purpose**: Fix old data with incorrect enum value  
**Status**: ✅ Created

### Migration 2: Add version column
**File**: `add-version-to-coupons.sql`  
**Purpose**: Enable optimistic locking for vouchers  
**Status**: ✅ Created

---

## Deployment Steps

1. **Backup database** before applying any changes
2. Run `fix-shipped-to-shipping.sql` migration
3. Run `add-version-to-coupons.sql` migration
4. Deploy new code with all 8 fixes
5. Restart application
6. Run test cases from checklist above
7. Monitor logs for any `ObjectOptimisticLockingFailureException` (should be rare and auto-retried)

---

## Performance Considerations

### Fix #1 (Duplicate Validation)
- **Impact**: Adds 1 additional query per refund request
- **Optimization**: Query is filtered and uses indexes on `order_id` and `status`

### Fix #5 (Voucher Double Restoration)
- **Impact**: Adds 1 additional query to check order history
- **Optimization**: Query is filtered and sorted, uses index on `order_id`

### Fix #8 (Optimistic Locking)
- **Impact**: Minimal - version check is part of UPDATE statement
- **Retry logic**: Only triggers on actual conflicts (rare in practice)
- **Recommendation**: Monitor retry frequency; if high, consider pessimistic locking

---

## Security Improvements

All 8 fixes contribute to security and fraud prevention:

1. ✅ Prevents refund fraud (duplicate refunds)
2. ✅ Prevents negative refund amounts
3. ✅ Enforces refund window policy
4. ✅ Maintains accurate financial records
5. ✅ Prevents voucher abuse
6. ✅ Enforces business rules via state machine
7. ✅ Fails safely when data is corrupted
8. ✅ Prevents voucher limit bypass via race conditions

---

## Conclusion

All 8 critical logic errors have been identified and fixed. The system now has:

- ✅ Robust validation for refund requests
- ✅ Accurate financial tracking
- ✅ Protection against fraud and abuse
- ✅ Safe fallback behavior
- ✅ Concurrency control for vouchers
- ✅ Proper state machine enforcement

**Next Steps**:
1. Apply database migrations
2. Deploy code changes
3. Run comprehensive testing
4. Monitor production logs for any issues

**Estimated Testing Time**: 2-3 hours  
**Risk Level**: Low (all changes are defensive and add validation)
