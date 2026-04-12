package com.codegym.smartphonemanagement.model;

public enum OrderStatus {
    PENDING,            // Đơn mới tạo, chờ admin xác nhận
    CONFIRMED,          // Admin đã xác nhận, chuẩn bị hàng
    SHIPPING,           // Đang vận chuyển
    DELIVERED,          // Đã giao thành công
    CANCELLED,          // Đã hủy (chỉ khi PENDING)
    REFUND_REQUESTED,   // Khách gửi yêu cầu hoàn trả
    REFUNDED,           // Hoàn trả toàn bộ
    PARTIAL_REFUNDED    // Hoàn trả một phần
}