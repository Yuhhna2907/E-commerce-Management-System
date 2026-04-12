package com.codegym.smartphonemanagement.service.coupon;

import com.codegym.smartphonemanagement.model.Coupon;
import com.codegym.smartphonemanagement.model.Order;
import com.codegym.smartphonemanagement.model.User;

import java.util.List;

public interface ICouponService {
    List<Coupon> getAvailableCoupons();
    List<Coupon> getUserWallet(User user);
    void saveToWallet(User user, String code);
    CouponValidationResult validateCoupon(String code, User user, Order orderDraft);
    void applyDiscountToOrder(Order order);
    void restoreVoucherUsage(String couponCode);
}
