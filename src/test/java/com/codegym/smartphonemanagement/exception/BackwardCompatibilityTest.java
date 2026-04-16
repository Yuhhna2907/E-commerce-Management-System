package com.codegym.smartphonemanagement.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify backward compatibility of BadRequestException and ResourceNotFoundException
 * after migration to extend BusinessException.
 */
class BackwardCompatibilityTest {

    @Test
    void testBadRequestException_BasicConstructor() {
        // Test that existing code using BadRequestException(String) still works
        String message = "Invalid request data";
        BadRequestException exception = new BadRequestException(message);
        
        // Verify message is preserved
        assertEquals(message, exception.getMessage());
        
        // Verify it's still a RuntimeException (for backward compatibility)
        assertTrue(exception instanceof RuntimeException);
        
        // Verify it now extends BusinessException
        assertTrue(exception instanceof BusinessException);
        
        // Verify HTTP status is 400
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
        
        // Verify error code is set
        assertEquals("BAD_REQUEST", exception.getErrorCode());
    }

    @Test
    void testBadRequestException_WithCause() {
        // Test new constructor with cause
        String message = "Invalid request data";
        Throwable cause = new IllegalArgumentException("Root cause");
        BadRequestException exception = new BadRequestException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void testBadRequestException_MethodChaining() {
        // Test that method chaining from BusinessException works
        BadRequestException exception = new BadRequestException("Test message");
        
        // Test withAdditionalInfo
        BusinessException result = exception.withAdditionalInfo("key", "value");
        assertSame(exception, result);
        assertTrue(exception.getAdditionalInfo().containsKey("key"));
        assertEquals("value", exception.getAdditionalInfo().get("key"));
        
        // Test withCorrelationId
        result = exception.withCorrelationId("correlation-123");
        assertSame(exception, result);
        assertTrue(exception.getAdditionalInfo().containsKey("correlationId"));
        assertEquals("correlation-123", exception.getAdditionalInfo().get("correlationId"));
    }

    @Test
    void testResourceNotFoundException_BasicConstructor() {
        // Test that existing code using ResourceNotFoundException(String) still works
        String message = "Resource not found";
        ResourceNotFoundException exception = new ResourceNotFoundException(message);
        
        // Verify message is preserved
        assertEquals(message, exception.getMessage());
        
        // Verify it's still a RuntimeException (for backward compatibility)
        assertTrue(exception instanceof RuntimeException);
        
        // Verify it now extends BusinessException
        assertTrue(exception instanceof BusinessException);
        
        // Verify HTTP status is 404
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        
        // Verify error code is set
        assertEquals("RESOURCE_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void testResourceNotFoundException_WithCause() {
        // Test new constructor with cause
        String message = "Resource not found";
        Throwable cause = new IllegalStateException("Root cause");
        ResourceNotFoundException exception = new ResourceNotFoundException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void testResourceNotFoundException_MethodChaining() {
        // Test that method chaining from BusinessException works
        ResourceNotFoundException exception = new ResourceNotFoundException("Test message");
        
        // Test withAdditionalInfo
        BusinessException result = exception.withAdditionalInfo("entityType", "Product");
        assertSame(exception, result);
        assertTrue(exception.getAdditionalInfo().containsKey("entityType"));
        assertEquals("Product", exception.getAdditionalInfo().get("entityType"));
        
        // Test withCorrelationId
        result = exception.withCorrelationId("correlation-456");
        assertSame(exception, result);
        assertTrue(exception.getAdditionalInfo().containsKey("correlationId"));
        assertEquals("correlation-456", exception.getAdditionalInfo().get("correlationId"));
    }

    @Test
    void testHttpStatusCodesRemainUnchanged() {
        // Verify that HTTP status codes are exactly as before
        BadRequestException badRequest = new BadRequestException("Bad request");
        assertEquals(400, badRequest.getHttpStatus().value());
        
        ResourceNotFoundException notFound = new ResourceNotFoundException("Not found");
        assertEquals(404, notFound.getHttpStatus().value());
    }

    @Test
    void testExceptionCanBeCaughtAsRuntimeException() {
        // Verify backward compatibility - code catching RuntimeException still works
        try {
            throw new BadRequestException("Test");
        } catch (RuntimeException e) {
            // Should catch successfully
            assertTrue(e instanceof BadRequestException);
        }
        
        try {
            throw new ResourceNotFoundException("Test");
        } catch (RuntimeException e) {
            // Should catch successfully
            assertTrue(e instanceof ResourceNotFoundException);
        }
    }

    @Test
    void testExceptionCanBeCaughtAsBusinessException() {
        // Verify new functionality - can now catch as BusinessException
        try {
            throw new BadRequestException("Test");
        } catch (BusinessException e) {
            // Should catch successfully
            assertTrue(e instanceof BadRequestException);
            assertEquals("BAD_REQUEST", e.getErrorCode());
        }
        
        try {
            throw new ResourceNotFoundException("Test");
        } catch (BusinessException e) {
            // Should catch successfully
            assertTrue(e instanceof ResourceNotFoundException);
            assertEquals("RESOURCE_NOT_FOUND", e.getErrorCode());
        }
    }
}
