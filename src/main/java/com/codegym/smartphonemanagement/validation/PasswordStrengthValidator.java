package com.codegym.smartphonemanagement.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validator implementation for @ValidPassword annotation.
 * Validates password strength according to security requirements.
 */
public class PasswordStrengthValidator implements ConstraintValidator<ValidPassword, String> {

    // Regex patterns for password validation
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?]");
    
    private static final int MIN_LENGTH = 8;

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // Null or empty passwords should be handled by @NotBlank
        if (password == null || password.isEmpty()) {
            return true;
        }

        List<String> failedCriteria = new ArrayList<>();

        // Check minimum length
        if (password.length() < MIN_LENGTH) {
            failedCriteria.add("tối thiểu " + MIN_LENGTH + " ký tự");
        }

        // Check for uppercase letter
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            failedCriteria.add("ít nhất một chữ cái viết hoa (A-Z)");
        }

        // Check for lowercase letter
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            failedCriteria.add("ít nhất một chữ cái viết thường (a-z)");
        }

        // Check for digit
        if (!DIGIT_PATTERN.matcher(password).find()) {
            failedCriteria.add("ít nhất một chữ số (0-9)");
        }

        // Check for special character
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            failedCriteria.add("ít nhất một ký tự đặc biệt (!@#$%^&*()_+-=[]{}|;:,.<>?)");
        }

        // If there are failed criteria, build custom error message
        if (!failedCriteria.isEmpty()) {
            context.disableDefaultConstraintViolation();
            String errorMessage = "Mật khẩu phải có: " + String.join(", ", failedCriteria);
            context.buildConstraintViolationWithTemplate(errorMessage)
                   .addConstraintViolation();
            return false;
        }

        return true;
    }
}
