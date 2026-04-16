package com.codegym.smartphonemanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested entity is not found in the system.
 * Returns HTTP 404 (Not Found) status code.
 */
public class EntityNotFoundException extends BusinessException {
    
    private static final String ERROR_CODE = "ENTITY_NOT_FOUND";
    private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;
    
    /**
     * Constructor with entity type and entity ID.
     * Automatically formats a descriptive error message.
     *
     * @param entityType The type of entity that was not found (e.g., "Product", "User", "Danh mục")
     * @param entityId   The ID of the entity that was not found
     */
    public EntityNotFoundException(String entityType, Object entityId) {
        super(ERROR_CODE, 
              String.format("%s với ID '%s' không tìm thấy", entityType, entityId), 
              HTTP_STATUS);
    }
    
    /**
     * Constructor with custom error message.
     *
     * @param message Custom error message describing what was not found
     */
    public EntityNotFoundException(String message) {
        super(ERROR_CODE, message, HTTP_STATUS);
    }
}
