package com.codegym.smartphonemanagement.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for password strength validation.
 * Validates that a password meets minimum security requirements:
 * - Minimum 8 characters
 * - At least one uppercase letter (A-Z)
 * - At least one lowercase letter (a-z)
 * - At least one digit (0-9)
 * - At least one special character from: !@#$%^&*()_+-=[]{}|;:,.<>?
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordStrengthValidator.class)
@Documented
public @interface ValidPassword {
    
    String message() default "Mật khẩu không đáp ứng yêu cầu độ mạnh";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
