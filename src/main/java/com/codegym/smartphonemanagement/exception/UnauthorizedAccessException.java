package com.codegym.smartphonemanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user attempts to access a resource without proper authorization.
 * This is a business exception that returns HTTP 403 (Forbidden).
 */
public class UnauthorizedAccessException extends BusinessException {
    
    private static final String ERROR_CODE = "UNAUTHORIZED_ACCESS";
    private static final HttpStatus HTTP_STATUS = HttpStatus.FORBIDDEN;
    
    /**
     * Constructor with resource name.
     * Creates a formatted message indicating unauthorized access to the specified resource.
     *
     * @param resource The name of the resource that the user attempted to access
     */
    public UnauthorizedAccessException(String resource) {
        super(
            ERROR_CODE,
            String.format("Unauthorized access to resource: '%s'", resource),
            HTTP_STATUS
        );
        this.withAdditionalInfo("resource", resource);
    }

}
