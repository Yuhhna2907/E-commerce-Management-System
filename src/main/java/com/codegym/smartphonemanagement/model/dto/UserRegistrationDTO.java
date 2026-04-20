package com.codegym.smartphonemanagement.model.dto;

import com.codegym.smartphonemanagement.validation.PasswordMatches;
import com.codegym.smartphonemanagement.validation.ValidPassword;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO for user registration with password confirmation
 */
@Data
@PasswordMatches(passwordField = "password", confirmPasswordField = "confirmPassword")
public class UserRegistrationDTO {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên không được quá 100 ký tự")
    private String fullName;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 4, max = 50, message = "Tên đăng nhập phải từ 4-50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84|84)[0-9]{9}$", message = "Số điện thoại không hợp lệ (VD: 0987654321 hoặc +84987654321)")
    private String phone;

    @NotBlank(message = "Mật khẩu không được để trống")
    @ValidPassword
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    private String confirmPassword;
}
