package com.codegym.smartphonemanagement.exception;

import com.codegym.smartphonemanagement.model.dto.ErrorResponse;
import com.codegym.smartphonemanagement.util.SecurityAuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Exception handler specifically for analytics controllers.
 * Handles analytics-specific exceptions with appropriate logging and responses.
 * 
 * Requirements: 14.2, 14.3, 14.5, 14.6
 */
@ControllerAdvice(basePackages = "com.codegym.smartphonemanagement.controller.admin.analytics")
@Slf4j
@RequiredArgsConstructor
public class AnalyticsExceptionHandler {

    private final SecurityAuditLogger securityAuditLogger;

    /**
     * Generate a unique trace ID for exception tracking
     * @return UUID string for correlation and debugging
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Get current authenticated username
     * @return Username or "anonymous" if not authenticated
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() 
            && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return "anonymous";
    }

    /**
     * Handle BadRequestException for analytics endpoints.
     * Returns HTTP 400 with Vietnamese error message.
     * 
     * Requirements: 14.2
     * 
     * @param ex The BadRequestException
     * @param request The HTTP request
     * @return ResponseEntity with error details or ModelAndView for HTML requests
     */
    @ExceptionHandler(BadRequestException.class)
    public Object handleBadRequestException(
            BadRequestException ex,
            HttpServletRequest request) {
        
        String traceId = generateTraceId();
        String username = getCurrentUsername();
        
        // Log at INFO level for bad requests (client error, not system error)
        log.info("Analytics bad request - User: {}, Message: {}, Path: {}, TraceId: {}", 
                username, ex.getMessage(), request.getRequestURI(), traceId);
        
        // Check if this is an AJAX/API request (expects JSON response)
        String acceptHeader = request.getHeader("Accept");
        boolean isJsonRequest = acceptHeader != null && acceptHeader.contains("application/json");
        
        if (isJsonRequest || request.getRequestURI().contains("/export/")) {
            // Return JSON response for API requests
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("timestamp", LocalDateTime.now());
            errorBody.put("status", HttpStatus.BAD_REQUEST.value());
            errorBody.put("error", "Bad Request");
            errorBody.put("message", ex.getMessage());
            errorBody.put("path", request.getRequestURI());
            errorBody.put("traceId", traceId);
            
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(errorBody);
        } else {
            // Return HTML error page for browser requests
            ModelAndView mav = new ModelAndView("error/400");
            mav.addObject("message", ex.getMessage());
            mav.addObject("traceId", traceId);
            mav.addObject("path", request.getRequestURI());
            mav.setStatus(HttpStatus.BAD_REQUEST);
            return mav;
        }
    }

    /**
     * Handle AccessDeniedException for analytics endpoints.
     * Returns HTTP 403 and redirects to login page.
     * Logs security audit event.
     * 
     * Requirements: 14.2, 14.3
     * 
     * @param ex The AccessDeniedException
     * @param request The HTTP request
     * @return ModelAndView redirecting to login or error page
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {
        
        String traceId = generateTraceId();
        String username = getCurrentUsername();
        
        // Log security audit event
        securityAuditLogger.logAccessDenied(
                username,
                request.getRequestURI(),
                "Attempted to access analytics endpoint without proper authorization"
        );
        
        // Log at WARNING level for access denied
        log.warn("Analytics access denied - User: {}, Path: {}, TraceId: {}", 
                username, request.getRequestURI(), traceId);
        
        // Redirect to login page if not authenticated
        if ("anonymous".equals(username)) {
            ModelAndView mav = new ModelAndView("redirect:/login");
            mav.addObject("error", "Vui lòng đăng nhập để truy cập trang này");
            return mav;
        }
        
        // Show 403 error page if authenticated but not authorized
        ModelAndView mav = new ModelAndView("error/403");
        mav.addObject("message", "Bạn không có quyền truy cập trang này");
        mav.addObject("traceId", traceId);
        mav.addObject("path", request.getRequestURI());
        mav.setStatus(HttpStatus.FORBIDDEN);
        return mav;
    }

    /**
     * Handle ResourceNotFoundException for analytics endpoints.
     * Returns HTTP 404 with Vietnamese error message.
     * 
     * Requirements: 14.2
     * 
     * @param ex The ResourceNotFoundException
     * @param request The HTTP request
     * @return ResponseEntity with error details or ModelAndView for HTML requests
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        String traceId = generateTraceId();
        String username = getCurrentUsername();
        
        // Log at INFO level for not found errors (client error)
        log.info("Analytics resource not found - User: {}, Message: {}, Path: {}, TraceId: {}", 
                username, ex.getMessage(), request.getRequestURI(), traceId);
        
        // Check if this is an AJAX/API request
        String acceptHeader = request.getHeader("Accept");
        boolean isJsonRequest = acceptHeader != null && acceptHeader.contains("application/json");
        
        if (isJsonRequest) {
            // Return JSON response for API requests
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("timestamp", LocalDateTime.now());
            errorBody.put("status", HttpStatus.NOT_FOUND.value());
            errorBody.put("error", "Not Found");
            errorBody.put("message", ex.getMessage());
            errorBody.put("path", request.getRequestURI());
            errorBody.put("traceId", traceId);
            
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(errorBody);
        } else {
            // Return HTML error page for browser requests
            ModelAndView mav = new ModelAndView("error/404");
            mav.addObject("message", ex.getMessage());
            mav.addObject("traceId", traceId);
            mav.addObject("path", request.getRequestURI());
            mav.setStatus(HttpStatus.NOT_FOUND);
            return mav;
        }
    }

    /**
     * Handle export-related exceptions (IOException, etc.).
     * Returns HTTP 500 with user-friendly Vietnamese message.
     * 
     * Requirements: 14.2, 14.5
     * 
     * @param ex The IOException
     * @param request The HTTP request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> handleExportException(
            IOException ex,
            HttpServletRequest request) {
        
        String traceId = generateTraceId();
        String username = getCurrentUsername();
        
        // Log at ERROR level for export failures (system error)
        log.error("Analytics export failed - User: {}, Path: {}, TraceId: {}, Error: {}", 
                username, request.getRequestURI(), traceId, ex.getMessage(), ex);
        
        // Return user-friendly error message
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("timestamp", LocalDateTime.now());
        errorBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorBody.put("error", "Export Failed");
        errorBody.put("message", "Không thể xuất file. Vui lòng thử lại sau hoặc liên hệ hỗ trợ với mã: " + traceId);
        errorBody.put("path", request.getRequestURI());
        errorBody.put("traceId", traceId);
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody);
    }

    /**
     * Handle generic RuntimeException for analytics endpoints.
     * Returns HTTP 500 with user-friendly Vietnamese message.
     * Logs full stack trace for debugging.
     * 
     * Requirements: 14.5, 14.6
     * 
     * @param ex The RuntimeException
     * @param request The HTTP request
     * @return ResponseEntity with error details or ModelAndView for HTML requests
     */
    @ExceptionHandler(RuntimeException.class)
    public Object handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {
        
        String traceId = generateTraceId();
        String username = getCurrentUsername();
        
        // Log at ERROR level with full stack trace
        log.error("Analytics unexpected error - User: {}, Path: {}, TraceId: {}, Error: {}", 
                username, request.getRequestURI(), traceId, ex.getMessage(), ex);
        
        // Check if this is an AJAX/API request
        String acceptHeader = request.getHeader("Accept");
        boolean isJsonRequest = acceptHeader != null && acceptHeader.contains("application/json");
        
        String userMessage = "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau hoặc liên hệ hỗ trợ với mã: " + traceId;
        
        if (isJsonRequest || request.getRequestURI().contains("/export/") 
            || request.getRequestURI().contains("/rebuild") 
            || request.getRequestURI().contains("/pin")) {
            // Return JSON response for API requests
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("timestamp", LocalDateTime.now());
            errorBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            errorBody.put("error", "Internal Server Error");
            errorBody.put("message", userMessage);
            errorBody.put("path", request.getRequestURI());
            errorBody.put("traceId", traceId);
            
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody);
        } else {
            // Return HTML error page for browser requests
            ModelAndView mav = new ModelAndView("error/500");
            mav.addObject("message", userMessage);
            mav.addObject("traceId", traceId);
            mav.addObject("path", request.getRequestURI());
            mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            return mav;
        }
    }

    /**
     * Handle generic Exception for analytics endpoints.
     * Returns HTTP 500 with user-friendly Vietnamese message.
     * This is the catch-all handler for any exception not handled by more specific handlers.
     * 
     * Requirements: 14.5, 14.6
     * 
     * @param ex The Exception
     * @param request The HTTP request
     * @return ResponseEntity with error details or ModelAndView for HTML requests
     */
    @ExceptionHandler(Exception.class)
    public Object handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        String traceId = generateTraceId();
        String username = getCurrentUsername();
        
        // Log at ERROR level with full stack trace
        log.error("Analytics critical error - Type: {}, User: {}, Path: {}, TraceId: {}, Error: {}", 
                ex.getClass().getName(), username, request.getRequestURI(), traceId, ex.getMessage(), ex);
        
        // Check if this is an AJAX/API request
        String acceptHeader = request.getHeader("Accept");
        boolean isJsonRequest = acceptHeader != null && acceptHeader.contains("application/json");
        
        String userMessage = "Đã xảy ra lỗi nghiêm trọng. Nhóm hỗ trợ đã được thông báo. Vui lòng thử lại sau hoặc liên hệ với mã: " + traceId;
        
        if (isJsonRequest || request.getRequestURI().contains("/export/") 
            || request.getRequestURI().contains("/rebuild") 
            || request.getRequestURI().contains("/pin")) {
            // Return JSON response for API requests
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("timestamp", LocalDateTime.now());
            errorBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            errorBody.put("error", "Internal Server Error");
            errorBody.put("message", userMessage);
            errorBody.put("path", request.getRequestURI());
            errorBody.put("traceId", traceId);
            
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody);
        } else {
            // Return HTML error page for browser requests
            ModelAndView mav = new ModelAndView("error/500");
            mav.addObject("message", userMessage);
            mav.addObject("traceId", traceId);
            mav.addObject("path", request.getRequestURI());
            mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            return mav;
        }
    }
}
