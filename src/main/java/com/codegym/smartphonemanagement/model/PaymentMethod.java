package com.codegym.smartphonemanagement.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public enum PaymentMethod {
        CASH("Thanh toán tiền mặt - COD"),
        TRANSFER("Chuyển khoản thủ công"),
        VNPAY("Thanh toán online qua VNPAY");

        private final String displayValue;

        PaymentMethod(String displayValue) {
            this.displayValue = displayValue;
        }

        public String getDisplayValue() {
            return displayValue;
        }
}
