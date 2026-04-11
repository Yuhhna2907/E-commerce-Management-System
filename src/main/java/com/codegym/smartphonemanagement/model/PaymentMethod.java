package com.codegym.smartphonemanagement.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public enum PaymentMethod {
        CASH("Thanh toán tiền mặt"),
        TRANSFER("Chuyển khoản ngân hàng"),
        CARD_AT_HOME("Cà thẻ tại nhà");

        private final String displayValue;

        PaymentMethod(String displayValue) {
            this.displayValue = displayValue;
        }

        public String getDisplayValue() {
            return displayValue;
        }
}
