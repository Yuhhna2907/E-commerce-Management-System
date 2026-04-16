package com.codegym.smartphonemanagement.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileUploadExceptionHandler
 */
class FileUploadExceptionHandlerTest {
    
    private FileUploadExceptionHandler exceptionHandler;
    
    @BeforeEach
    void setUp() {
        exceptionHandler = new FileUploadExceptionHandler();
    }
    
    @Test
    void testHandleInvalidFileException() {
        // Given
        String errorMessage = "File không hợp lệ";
        InvalidFileException exception = new InvalidFileException(errorMessage);
        
        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleInvalidFileException(exception);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.get("status"));
        assertEquals("Invalid File", body.get("error"));
        assertEquals(errorMessage, body.get("message"));
        assertNotNull(body.get("timestamp"));
    }
    
    @Test
    void testHandleMaxUploadSizeExceededException() {
        // Given
        MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(5 * 1024 * 1024);
        
        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleMaxUploadSizeExceededException(exception);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE.value(), body.get("status"));
        assertEquals("File Too Large", body.get("error"));
        assertTrue(body.get("message").toString().contains("5MB"));
        assertNotNull(body.get("timestamp"));
    }
    
    @Test
    void testHandleIOException() {
        // Given
        String errorMessage = "Lỗi đọc file";
        IOException exception = new IOException(errorMessage);
        
        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleIOException(exception);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.get("status"));
        assertEquals("File Processing Error", body.get("error"));
        assertTrue(body.get("message").toString().contains("Lỗi khi xử lý file"));
        assertNotNull(body.get("timestamp"));
    }
    
    @Test
    void testInvalidFileExceptionWithCause() {
        // Given
        IOException cause = new IOException("Root cause");
        InvalidFileException exception = new InvalidFileException("Wrapper message", cause);
        
        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleInvalidFileException(exception);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Wrapper message", response.getBody().get("message"));
    }
}
