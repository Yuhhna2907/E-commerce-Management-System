package com.codegym.smartphonemanagement.exception;

import com.codegym.smartphonemanagement.model.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler
 * Tests the BusinessException handler implementation
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest mockRequest;

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler(mockRequest);
    }

    @Test
    void handleBusinessException_ShouldReturnErrorResponseWithCorrectStatus() {
        // Given
        String requestPath = "/api/products/123";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        EntityNotFoundException exception = new EntityNotFoundException("Product", 123);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void handleBusinessException_ShouldIncludeAllRequiredFields() {
        // Given
        String requestPath = "/api/products/123";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        EntityNotFoundException exception = new EntityNotFoundException("Product", 123);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(exception, mockRequest);

        // Then
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(404);
        assertThat(errorResponse.getError()).isEqualTo("Not Found");
        assertThat(errorResponse.getMessage()).isEqualTo("Product with ID '123' not found");
        assertThat(errorResponse.getPath()).isEqualTo(requestPath);
        assertThat(errorResponse.getTraceId()).isNotNull();
    }

    @Test
    void handleBusinessException_ShouldIncludeAdditionalInfo() {
        // Given
        String requestPath = "/api/orders/456";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        EntityNotFoundException exception = new EntityNotFoundException("Order", 456)
                .withAdditionalInfo("userId", "user123")
                .withCorrelationId("corr-456");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(exception, mockRequest);

        // Then
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getAdditionalInfo()).isNotNull();
        assertThat(errorResponse.getAdditionalInfo()).containsEntry("userId", "user123");
        assertThat(errorResponse.getAdditionalInfo()).containsEntry("correlationId", "corr-456");
    }

    @Test
    void handleBusinessException_ShouldGenerateUniqueTraceIds() {
        // Given
        String requestPath = "/api/test";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        EntityNotFoundException exception1 = new EntityNotFoundException("Entity", 1);
        EntityNotFoundException exception2 = new EntityNotFoundException("Entity", 2);

        // When
        ResponseEntity<ErrorResponse> response1 = exceptionHandler.handleBusinessException(exception1, mockRequest);
        ResponseEntity<ErrorResponse> response2 = exceptionHandler.handleBusinessException(exception2, mockRequest);

        // Then
        String traceId1 = response1.getBody().getTraceId();
        String traceId2 = response2.getBody().getTraceId();
        assertThat(traceId1).isNotEqualTo(traceId2);
    }

    @Test
    void handleBusinessException_ShouldExtractErrorCodeFromException() {
        // Given
        String requestPath = "/api/products/999";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        EntityNotFoundException exception = new EntityNotFoundException("Product", 999);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(exception, mockRequest);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo("ENTITY_NOT_FOUND");
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void handleValidationException_ShouldReturnBadRequestStatus() {
        // Given
        String requestPath = "/api/products";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("product", "name", "Name is required");
        FieldError fieldError2 = new FieldError("product", "price", "Price must be positive");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleValidationException_ShouldIncludeFieldErrors() {
        // Given
        String requestPath = "/api/products";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("product", "name", "Name is required");
        FieldError fieldError2 = new FieldError("product", "price", "Price must be positive");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(exception, mockRequest);

        // Then
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getFieldErrors()).isNotNull();
        assertThat(errorResponse.getFieldErrors()).hasSize(2);
        assertThat(errorResponse.getFieldErrors()).containsEntry("name", "Name is required");
        assertThat(errorResponse.getFieldErrors()).containsEntry("price", "Price must be positive");
    }

    @Test
    void handleValidationException_ShouldIncludeAllRequiredFields() {
        // Given
        String requestPath = "/api/products";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("product", "name", "Name is required");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(exception, mockRequest);

        // Then
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(400);
        assertThat(errorResponse.getError()).isEqualTo("Bad Request");
        assertThat(errorResponse.getMessage()).isEqualTo("Validation failed for one or more fields");
        assertThat(errorResponse.getPath()).isEqualTo(requestPath);
        assertThat(errorResponse.getTraceId()).isNotNull();
    }

    @Test
    void handleValidationException_ShouldGenerateUniqueTraceId() {
        // Given
        String requestPath = "/api/products";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        BindingResult bindingResult1 = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("product", "name", "Name is required");
        when(bindingResult1.getFieldErrors()).thenReturn(List.of(fieldError1));
        
        BindingResult bindingResult2 = mock(BindingResult.class);
        FieldError fieldError2 = new FieldError("product", "price", "Price is required");
        when(bindingResult2.getFieldErrors()).thenReturn(List.of(fieldError2));
        
        MethodArgumentNotValidException exception1 = new MethodArgumentNotValidException(null, bindingResult1);
        MethodArgumentNotValidException exception2 = new MethodArgumentNotValidException(null, bindingResult2);

        // When
        ResponseEntity<ErrorResponse> response1 = exceptionHandler.handleValidationException(exception1, mockRequest);
        ResponseEntity<ErrorResponse> response2 = exceptionHandler.handleValidationException(exception2, mockRequest);

        // Then
        String traceId1 = response1.getBody().getTraceId();
        String traceId2 = response2.getBody().getTraceId();
        assertThat(traceId1).isNotNull();
        assertThat(traceId2).isNotNull();
        assertThat(traceId1).isNotEqualTo(traceId2);
    }

    @Test
    void handleValidationException_ShouldHandleEmptyFieldErrors() {
        // Given
        String requestPath = "/api/products";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(exception, mockRequest);

        // Then
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getFieldErrors()).isNotNull();
        assertThat(errorResponse.getFieldErrors()).isEmpty();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleRuntimeException_ShouldReturnInternalServerErrorStatus() {
        // Given
        String requestPath = "/api/test";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        RuntimeException exception = new RuntimeException("Unexpected error occurred");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRuntimeException(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void handleRuntimeException_ShouldIncludeAllRequiredFields() {
        // Given
        String requestPath = "/api/test";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        RuntimeException exception = new RuntimeException("Unexpected error occurred");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRuntimeException(exception, mockRequest);

        // Then
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(500);
        assertThat(errorResponse.getError()).isEqualTo("Internal Server Error");
        assertThat(errorResponse.getMessage()).isNotNull();
        assertThat(errorResponse.getPath()).isEqualTo(requestPath);
        assertThat(errorResponse.getTraceId()).isNotNull();
    }

    @Test
    void handleRuntimeException_ShouldSanitizeErrorMessage() {
        // Given
        String requestPath = "/api/test";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        RuntimeException exception = new RuntimeException("Database connection failed: password=secret123");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRuntimeException(exception, mockRequest);

        // Then
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        // Message should be sanitized and not expose internal details
        assertThat(errorResponse.getMessage()).doesNotContain("Database connection failed");
        assertThat(errorResponse.getMessage()).doesNotContain("password");
        assertThat(errorResponse.getMessage()).doesNotContain("secret123");
        assertThat(errorResponse.getMessage()).contains("unexpected error occurred");
        assertThat(errorResponse.getMessage()).contains(errorResponse.getTraceId());
    }

    @Test
    void handleRuntimeException_ShouldGenerateUniqueTraceId() {
        // Given
        String requestPath = "/api/test";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        RuntimeException exception1 = new RuntimeException("Error 1");
        RuntimeException exception2 = new RuntimeException("Error 2");

        // When
        ResponseEntity<ErrorResponse> response1 = exceptionHandler.handleRuntimeException(exception1, mockRequest);
        ResponseEntity<ErrorResponse> response2 = exceptionHandler.handleRuntimeException(exception2, mockRequest);

        // Then
        String traceId1 = response1.getBody().getTraceId();
        String traceId2 = response2.getBody().getTraceId();
        assertThat(traceId1).isNotNull();
        assertThat(traceId2).isNotNull();
        assertThat(traceId1).isNotEqualTo(traceId2);
    }

    @Test
    void handleRuntimeException_ShouldIncludeTraceIdInMessage() {
        // Given
        String requestPath = "/api/test";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        RuntimeException exception = new RuntimeException("Unexpected error");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRuntimeException(exception, mockRequest);

        // Then
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getMessage()).contains(errorResponse.getTraceId());
        assertThat(errorResponse.getMessage()).contains("trace ID");
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerErrorStatus() {
        // Given
        String requestPath = "/api/test";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        Exception exception = new Exception("Generic checked exception");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void handleGenericException_ShouldIncludeAllRequiredFields() {
        // Given
        String requestPath = "/api/test";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        Exception exception = new Exception("Generic checked exception");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception, mockRequest);

        // Then
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(500);
        assertThat(errorResponse.getError()).isEqualTo("Internal Server Error");
        assertThat(errorResponse.getMessage()).isNotNull();
        assertThat(errorResponse.getPath()).isEqualTo(requestPath);
        assertThat(errorResponse.getTraceId()).isNotNull();
    }

    @Test
    void handleGenericException_ShouldExcludeSensitiveInformation() {
        // Given
        String requestPath = "/api/test";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        Exception exception = new Exception("SQL Error: Connection string jdbc://user:password@localhost");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception, mockRequest);

        // Then
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        // Message should not expose sensitive information
        assertThat(errorResponse.getMessage()).doesNotContain("SQL Error");
        assertThat(errorResponse.getMessage()).doesNotContain("password");
        assertThat(errorResponse.getMessage()).doesNotContain("jdbc://");
        assertThat(errorResponse.getMessage()).contains("something went wrong");
        assertThat(errorResponse.getMessage()).contains(errorResponse.getTraceId());
    }

    // ========== Backward Compatibility Tests ==========
    // Requirements: 5.3, 5.6 - Verify existing exception types are handled correctly

    @Test
    void handleBusinessException_ShouldHandleBadRequestException() {
        // Given
        String requestPath = "/api/products";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        BadRequestException exception = new BadRequestException("Invalid product data");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid product data");
        assertThat(response.getBody().getPath()).isEqualTo(requestPath);
        assertThat(response.getBody().getTraceId()).isNotNull();
    }

    @Test
    void handleBusinessException_ShouldHandleResourceNotFoundException() {
        // Given
        String requestPath = "/api/products/999";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        ResourceNotFoundException exception = new ResourceNotFoundException("Product not found");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo("Product not found");
        assertThat(response.getBody().getPath()).isEqualTo(requestPath);
        assertThat(response.getBody().getTraceId()).isNotNull();
    }

    @Test
    void handleBusinessException_ShouldVerifyBadRequestExceptionErrorCode() {
        // Given
        String requestPath = "/api/test";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        BadRequestException exception = new BadRequestException("Test error");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(exception, mockRequest);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo("BAD_REQUEST");
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleBusinessException_ShouldVerifyResourceNotFoundExceptionErrorCode() {
        // Given
        String requestPath = "/api/test";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        ResourceNotFoundException exception = new ResourceNotFoundException("Test error");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(exception, mockRequest);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleBusinessException_ShouldHandleBadRequestExceptionWithCause() {
        // Given
        String requestPath = "/api/test";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        Throwable cause = new IllegalArgumentException("Invalid argument");
        BadRequestException exception = new BadRequestException("Bad request with cause", cause);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Bad request with cause");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void handleBusinessException_ShouldHandleResourceNotFoundExceptionWithCause() {
        // Given
        String requestPath = "/api/test";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        Throwable cause = new IllegalStateException("Invalid state");
        ResourceNotFoundException exception = new ResourceNotFoundException("Resource not found with cause", cause);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Resource not found with cause");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void handleBusinessException_ShouldHandleBadRequestExceptionWithMethodChaining() {
        // Given
        String requestPath = "/api/test";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        BadRequestException exception = new BadRequestException("Test error")
                .withAdditionalInfo("field", "value")
                .withCorrelationId("corr-123");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getAdditionalInfo()).containsEntry("field", "value");
        assertThat(errorResponse.getAdditionalInfo()).containsEntry("correlationId", "corr-123");
    }

    @Test
    void handleBusinessException_ShouldHandleResourceNotFoundExceptionWithMethodChaining() {
        // Given
        String requestPath = "/api/test";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        ResourceNotFoundException exception = new ResourceNotFoundException("Test error")
                .withAdditionalInfo("resourceId", "123")
                .withCorrelationId("corr-456");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getAdditionalInfo()).containsEntry("resourceId", "123");
        assertThat(errorResponse.getAdditionalInfo()).containsEntry("correlationId", "corr-456");
    }

    @Test
    void handleBusinessException_ShouldVerifyAllBusinessExceptionSubtypesAreCaught() {
        // Given
        String requestPath = "/api/test";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        // Test all BusinessException subtypes
        BusinessException[] exceptions = {
            new EntityNotFoundException("Entity", 1),
            new InsufficientStockException("Product", 10, 5),
            new InvalidOrderStatusException("PENDING", "CANCELLED"),
            new UnauthorizedAccessException("Resource"),
            new BadRequestException("Bad request"),
            new ResourceNotFoundException("Resource not found")
        };

        // When & Then
        for (BusinessException exception : exceptions) {
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(exception, mockRequest);
            
            assertThat(response).isNotNull();
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(exception.getHttpStatus());
            assertThat(response.getBody().getStatus()).isEqualTo(exception.getHttpStatus().value());
            assertThat(response.getBody().getError()).isEqualTo(exception.getHttpStatus().getReasonPhrase());
            assertThat(response.getBody().getMessage()).isEqualTo(exception.getMessage());
            assertThat(response.getBody().getTraceId()).isNotNull();
        }
    }

    @Test
    void handleBusinessException_ShouldMaintainAPIContractForExistingExceptions() {
        // Given
        String requestPath = "/api/products/123";
        when(mockRequest.getRequestURI()).thenReturn(requestPath);
        
        // Test that existing exceptions maintain their API contract
        BadRequestException badRequest = new BadRequestException("Invalid data");
        ResourceNotFoundException notFound = new ResourceNotFoundException("Not found");

        // When
        ResponseEntity<ErrorResponse> badRequestResponse = exceptionHandler.handleBusinessException(badRequest, mockRequest);
        ResponseEntity<ErrorResponse> notFoundResponse = exceptionHandler.handleBusinessException(notFound, mockRequest);

        // Then - Verify response structure matches expected API contract
        // BadRequestException should return 400
        assertThat(badRequestResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badRequestResponse.getBody().getStatus()).isEqualTo(400);
        assertThat(badRequestResponse.getBody().getError()).isEqualTo("Bad Request");
        
        // ResourceNotFoundException should return 404
        assertThat(notFoundResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(notFoundResponse.getBody().getStatus()).isEqualTo(404);
        assertThat(notFoundResponse.getBody().getError()).isEqualTo("Not Found");
        
        // Both should have consistent response structure
        assertThat(badRequestResponse.getBody().getTimestamp()).isNotNull();
        assertThat(badRequestResponse.getBody().getMessage()).isNotNull();
        assertThat(badRequestResponse.getBody().getPath()).isEqualTo(requestPath);
        assertThat(badRequestResponse.getBody().getTraceId()).isNotNull();
        
        assertThat(notFoundResponse.getBody().getTimestamp()).isNotNull();
        assertThat(notFoundResponse.getBody().getMessage()).isNotNull();
        assertThat(notFoundResponse.getBody().getPath()).isEqualTo(requestPath);
        assertThat(notFoundResponse.getBody().getTraceId()).isNotNull();
    }
}
