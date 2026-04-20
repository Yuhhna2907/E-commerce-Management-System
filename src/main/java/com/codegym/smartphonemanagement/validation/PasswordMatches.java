package com.codegym.smartphonemanagement.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation to check if password and confirmPassword fields match.
 * This annotation should be applied at the class level.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordMatchesValidator.class)
@Documented
public @interface PasswordMatches {
    
    String message() default "Mật khẩu và xác nhận mật khẩu không khớp";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * The name of the password field
     */
    String passwordField() default "password";
    
    /**
     * The name of the confirm password field
     */
    String confirmPasswordField() default "confirmPassword";
}
