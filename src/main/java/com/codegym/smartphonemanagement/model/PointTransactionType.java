package com.codegym.smartphonemanagement.model;

public enum PointTransactionType {
    EARNED,           // Tích điểm từ đơn hàng DELIVERED
    REDEEMED,         // Tiêu điểm để đổi coupon ưu đãi
    REFUND_DEDUCTED,  // Trừ điểm khi đơn hàng bị hoàn trả
    ADMIN_ADJUST      // Admin điều chỉnh điểm thủ công
}
