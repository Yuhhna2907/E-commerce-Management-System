package com.codegym.smartphonemanagement.model;

/**
 * Loại giao dịch Ví SmartZone Xu
 */
public enum WalletTransactionType {

    /** Admin bơm tiền vào ví (bồi thường, nạp hộ) */
    ADMIN_TOP_UP,

    /** Hoàn tiền từ đơn huỷ / COD trả hàng vào ví */
    REFUND_IN,

    /** Thanh toán đơn hàng bằng SmartZone Xu */
    PAYMENT,

    /** User yêu cầu rút tiền ra Ngân Hàng */
    WITHDRAWAL
}
