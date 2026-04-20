package com.codegym.smartphonemanagement.model;

public enum WithdrawalStatus {
    /** Chờ Admin duyệt */
    PENDING,
    /** Admin đã duyệt & chuyển khoản thực */
    APPROVED,
    /** Admin từ chối */
    REJECTED
}
