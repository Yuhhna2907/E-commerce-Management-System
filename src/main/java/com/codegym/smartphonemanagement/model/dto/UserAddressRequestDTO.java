package com.codegym.smartphonemanagement.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserAddressRequestDTO {

    @Size(max = 50, message = "Nhãn tối đa 50 ký tự")
    private String label;

    @NotBlank(message = "Vui lòng nhập tên người nhận")
    @Size(max = 100)
    private String receiverName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải có 10 chữ số bắt đầu bằng 0")
    private String receiverPhone;

    @Size(max = 255)
    private String addressDetail;

    @Size(max = 100)
    private String ward;

    @Size(max = 100)
    private String district;

    @Size(max = 100)
    private String province;

    private Boolean isDefault = false;
}
