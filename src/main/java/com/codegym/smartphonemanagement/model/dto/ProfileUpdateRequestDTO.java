package com.codegym.smartphonemanagement.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ProfileUpdateRequestDTO {

    @Size(max = 100, message = "Tên hiển thị tối đa 100 ký tự")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    @Size(max = 100)
    private String email;

    @Pattern(regexp = "^(0[0-9]{9})$", message = "Số điện thoại phải có 10 chữ số bắt đầu bằng 0")
    private String phone;

    private LocalDate dateOfBirth;

    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Giới tính không hợp lệ")
    private String gender;

    @Size(max = 500, message = "URL avatar tối đa 500 ký tự")
    private String avatarUrl;
}
