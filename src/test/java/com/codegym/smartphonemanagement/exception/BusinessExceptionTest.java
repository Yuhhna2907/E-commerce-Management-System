package com.codegym.smartphonemanagement.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BusinessException method chaining functionality.
 */
class BusinessExceptionTest {
    
    /**
     * Test exception class for testing purposes.
     */
    private static class TestBusinessException extends BusinessException {
        public TestBusinessException(String errorCode, String message, HttpStatus httpStatus) {
            super(errorCode, message, httpStatus);
        }
    }
    
    @Test
    void testWithAdditionalInfo_shouldAddKeyValuePair() {
        // Given
        TestBusinessException exception = new TestBusinessException(
            "TEST_ERROR", 
            "Test message", 
            HttpStatus.BAD_REQUEST
        );
        
        // When
        BusinessException result = exception.withAdditionalInfo("key1", "value1");
        
        // Then
        assertSame(exception, result, "Should return same instance for chaining");
        assertTrue(exception.getAdditionalInfo().containsKey("key1"));
        assertEquals("value1", exception.getAdditionalInfo().get("key1"));
    }
    
    @Test
    void testWithAdditionalInfo_shouldSupportChaining() {
        // Given
        TestBusinessException exception = new TestBusinessException(
            "TEST_ERROR", 
            "Test message", 
            HttpStatus.BAD_REQUEST
        );
        
        // When
        BusinessException result = exception
            .withAdditionalInfo("key1", "value1")
            .withAdditionalInfo("key2", 123)
            .withAdditionalInfo("key3", true);
        
        // Then
        assertSame(exception, result, "Should return same instance for chaining");
        assertEquals(3, exception.getAdditionalInfo().size());
        assertEquals("value1", exception.getAdditionalInfo().get("key1"));
        assertEquals(123, exception.getAdditionalInfo().get("key2"));
        assertEquals(true, exception.getAdditionalInfo().get("key3"));
    }
    
    @Test
    void testWithCorrelationId_shouldAddCorrelationIdToAdditionalInfo() {
        // Given
        TestBusinessException exception = new TestBusinessException(
            "TEST_ERROR", 
            "Test message", 
            HttpStatus.BAD_REQUEST
        );
        String correlationId = "test-correlation-id-123";
        
        // When
        BusinessException result = exception.withCorrelationId(correlationId);
        
        // Then
        assertSame(exception, result, "Should return same instance for chaining");
        assertTrue(exception.getAdditionalInfo().containsKey("correlationId"));
        assertEquals(correlationId, exception.getAdditionalInfo().get("correlationId"));
    }
    
    @Test
    void testMethodChaining_shouldSupportMixedChaining() {
        // Given
        TestBusinessException exception = new TestBusinessException(
            "TEST_ERROR", 
            "Test message", 
            HttpStatus.BAD_REQUEST
        );
        String correlationId = "correlation-123";
        
        // When
        BusinessException result = exception
            .withAdditionalInfo("userId", "user-456")
            .withCorrelationId(correlationId)
            .withAdditionalInfo("action", "checkout");
        
        // Then
        assertSame(exception, result, "Should return same instance for chaining");
        assertEquals(3, exception.getAdditionalInfo().size());
        assertEquals("user-456", exception.getAdditionalInfo().get("userId"));
        assertEquals(correlationId, exception.getAdditionalInfo().get("correlationId"));
        assertEquals("checkout", exception.getAdditionalInfo().get("action"));
    }
    
    @Test
    void testWithAdditionalInfo_shouldOverwriteExistingKey() {
        // Given
        TestBusinessException exception = new TestBusinessException(
            "TEST_ERROR", 
            "Test message", 
            HttpStatus.BAD_REQUEST
        );
        
        // When
        exception
            .withAdditionalInfo("key1", "value1")
            .withAdditionalInfo("key1", "value2");
        
        // Then
        assertEquals("value2", exception.getAdditionalInfo().get("key1"));
    }
    
    @Test
    void testGetAdditionalInfo_shouldReturnCopy() {
        // Given
        TestBusinessException exception = new TestBusinessException(
            "TEST_ERROR", 
            "Test message", 
            HttpStatus.BAD_REQUEST
        );
        exception.withAdditionalInfo("key1", "value1");
        
        // When
        var info1 = exception.getAdditionalInfo();
        var info2 = exception.getAdditionalInfo();
        
        // Then
        assertNotSame(info1, info2, "Should return a new copy each time");
        assertEquals(info1, info2, "Copies should have same content");
    }
}
