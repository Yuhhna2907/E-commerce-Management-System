package com.codegym.smartphonemanagement.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;

/**
 * Validator implementation for @PasswordMatches annotation.
 * Validates that password and confirmPassword fields have the same value.
 */
public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    private String passwordField;
    private String confirmPasswordField;

    @Override
    public void initialize(PasswordMatches constraintAnnotation) {
        this.passwordField = constraintAnnotation.passwordField();
        this.confirmPasswordField = constraintAnnotation.confirmPasswordField();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true;
        }

        try {
            // Get password field value
            Field passwordFieldObj = obj.getClass().getDeclaredField(passwordField);
            passwordFieldObj.setAccessible(true);
            String password = (String) passwordFieldObj.get(obj);

            // Get confirmPassword field value
            Field confirmPasswordFieldObj = obj.getClass().getDeclaredField(confirmPasswordField);
            confirmPasswordFieldObj.setAccessible(true);
            String confirmPassword = (String) confirmPasswordFieldObj.get(obj);

            // Check if both are null or both match
            if (password == null && confirmPassword == null) {
                return true;
            }

            if (password == null || confirmPassword == null) {
                return false;
            }

            return password.equals(confirmPassword);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            // If fields don't exist, validation fails
            return false;
        }
    }
}
