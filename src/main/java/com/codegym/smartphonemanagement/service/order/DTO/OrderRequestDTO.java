package com.codegym.smartphonemanagement.service.order.DTO;

import com.codegym.smartphonemanagement.model.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OrderRequestDTO {
    @NotBlank(message = "Vui lòng cho biết tên người nhận hàng")
    private String customerName;

    @NotBlank(message = "Vui lòng cho biết số điện thoại khách hàng")
    @Pattern(regexp = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$",
            message = "Vui lòng nhập số điện thoại hợp lệ") // Giữ cái Regex của m
    private String receiverPhone;

    @NotBlank(message = "Vui lòng chọn Tỉnh/Thành phố")
    private String province;

    @NotBlank(message = "Vui lòng chọn Quận/Huyện")
    private String district;

    @NotBlank(message = "Vui lòng chọn Phường/Xã")
    private String ward;

    @NotBlank(message = "Vui lòng nhập số nhà, tên đường")
    private String addressDetail;

    private String note;

    @NotNull(message = "Vui lòng chọn phương thức thanh toán")
    private PaymentMethod paymentMethod;

    private String couponCode;
    private String shippingCouponCode;
}
