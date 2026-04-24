package com.codegym.smartphonemanagement.model;

public enum AdminNotificationType {
    NEW_ORDER,          // Thông báo khi có đơn hàng mới
    REFUND_REQUEST,     // Khách yêu cầu hoàn tiền
    WITHDRAWAL_REQUEST, // Yêu cầu rút tiền từ ví
    NEW_QUESTION,       // Có câu hỏi sản phẩm mới
    LOW_STOCK,          // Cảnh báo sản phẩm sắp hết hàng
    SECURITY_ALERT      // Cảnh báo bảo mật (brute-force, v.v.)
}
