package com.codegym.smartphonemanagement.model;

public enum CouponCategory {
    PRODUCT_DISCOUNT("Giảm giá sản phẩm"),
    ORDER_DISCOUNT("Giảm giá toàn đơn"),
    FREE_SHIPPING("Miễn phí vận chuyển");

    private final String description;

    CouponCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
