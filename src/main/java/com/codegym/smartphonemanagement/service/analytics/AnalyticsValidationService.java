package com.codegym.smartphonemanagement.service.analytics;

import com.codegym.smartphonemanagement.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;

/**
 * Service for validating analytics-related input parameters.
 * Provides centralized validation logic for search keywords, date ranges, and pagination.
 * 
 * Requirements: 7.5, 13.1, 13.2, 13.3
 */
@Service
@Slf4j
public class AnalyticsValidationService {

    private static final int MIN_KEYWORD_LENGTH = 2;
    private static final int MAX_KEYWORD_LENGTH = 255;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_DATE_RANGE_YEARS = 1;

    /**
     * Validates and sanitizes a search keyword.
     * 
     * Validation rules:
     * - Keyword must not be null or empty
     * - Length must be between 2 and 255 characters
     * - Sanitizes for XSS prevention using HtmlUtils.htmlEscape()
     * - Converts to lowercase for consistent aggregation
     * 
     * Requirements: 7.5, 13.1
     * 
     * @param keyword The search keyword to validate
     * @return Sanitized and lowercase keyword
     * @throws BadRequestException if validation fails
     */
    public String validateSearchKeyword(String keyword) {
        log.debug("Validating search keyword: {}", keyword);
        
        if (keyword == null || keyword.trim().isEmpty()) {
            log.warn("Search keyword validation failed: keyword is null or empty");
            throw new BadRequestException("Từ khóa tìm kiếm không được để trống");
        }
        
        String trimmedKeyword = keyword.trim();
        
        if (trimmedKeyword.length() < MIN_KEYWORD_LENGTH) {
            log.warn("Search keyword validation failed: keyword too short (length: {})", trimmedKeyword.length());
            throw new BadRequestException(
                String.format("Từ khóa tìm kiếm phải có ít nhất %d ký tự", MIN_KEYWORD_LENGTH)
            );
        }
        
        if (trimmedKeyword.length() > MAX_KEYWORD_LENGTH) {
            log.warn("Search keyword validation failed: keyword too long (length: {})", trimmedKeyword.length());
            throw new BadRequestException(
                String.format("Từ khóa tìm kiếm không được vượt quá %d ký tự", MAX_KEYWORD_LENGTH)
            );
        }
        
        // Sanitize for XSS prevention
        String sanitized = HtmlUtils.htmlEscape(trimmedKeyword);
        
        // Convert to lowercase for consistent aggregation
        String result = sanitized.toLowerCase();
        
        log.debug("Search keyword validated successfully: {} -> {}", keyword, result);
        return result;
    }

    /**
     * Validates a date range for analytics queries.
     * 
     * Validation rules:
     * - If both dates are provided, start date must be before or equal to end date
     * - Start date cannot be more than 1 year in the past
     * - End date cannot be in the future
     * 
     * Requirements: 13.1, 13.2
     * 
     * @param startDate The start date of the range (nullable)
     * @param endDate The end date of the range (nullable)
     * @throws BadRequestException if validation fails
     */
    public void validateDateRange(LocalDate startDate, LocalDate endDate) {
        log.debug("Validating date range: startDate={}, endDate={}", startDate, endDate);
        
        LocalDate today = LocalDate.now();
        
        // Validate start date is not in the future
        if (startDate != null && startDate.isAfter(today)) {
            log.warn("Date range validation failed: start date is in the future ({})", startDate);
            throw new BadRequestException("Ngày bắt đầu không được là ngày trong tương lai");
        }
        
        // Validate end date is not in the future
        if (endDate != null && endDate.isAfter(today)) {
            log.warn("Date range validation failed: end date is in the future ({})", endDate);
            throw new BadRequestException("Ngày kết thúc không được là ngày trong tương lai");
        }
        
        // Validate start date is before or equal to end date
        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                log.warn("Date range validation failed: start date ({}) is after end date ({})", startDate, endDate);
                throw new BadRequestException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
            }
        }
        
        // Validate start date is not more than 1 year in the past
        if (startDate != null) {
            LocalDate oneYearAgo = today.minusYears(MAX_DATE_RANGE_YEARS);
            if (startDate.isBefore(oneYearAgo)) {
                log.warn("Date range validation failed: start date ({}) is more than {} year(s) in the past", 
                    startDate, MAX_DATE_RANGE_YEARS);
                throw new BadRequestException(
                    String.format("Ngày bắt đầu không được quá %d năm trong quá khứ", MAX_DATE_RANGE_YEARS)
                );
            }
        }
        
        log.debug("Date range validated successfully");
    }

    /**
     * Validates pagination parameters.
     * 
     * Validation rules:
     * - Page number must be >= 0
     * - Page size must be between 1 and 100
     * 
     * Requirements: 13.1, 13.3
     * 
     * @param page The page number (0-indexed)
     * @param size The page size
     * @throws BadRequestException if validation fails
     */
    public void validatePagination(int page, int size) {
        log.debug("Validating pagination: page={}, size={}", page, size);
        
        if (page < 0) {
            log.warn("Pagination validation failed: page number is negative ({})", page);
            throw new BadRequestException("Số trang phải lớn hơn hoặc bằng 0");
        }
        
        if (size < MIN_PAGE_SIZE) {
            log.warn("Pagination validation failed: page size too small ({})", size);
            throw new BadRequestException(
                String.format("Kích thước trang phải lớn hơn hoặc bằng %d", MIN_PAGE_SIZE)
            );
        }
        
        if (size > MAX_PAGE_SIZE) {
            log.warn("Pagination validation failed: page size too large ({})", size);
            throw new BadRequestException(
                String.format("Kích thước trang không được vượt quá %d", MAX_PAGE_SIZE)
            );
        }
        
        log.debug("Pagination validated successfully");
    }

    /**
     * Validates a category ID.
     * 
     * Validation rules:
     * - Category ID must be positive if provided
     * 
     * Requirements: 13.1
     * 
     * @param categoryId The category ID to validate (nullable)
     * @throws BadRequestException if validation fails
     */
    public void validateCategoryId(Long categoryId) {
        if (categoryId != null && categoryId <= 0) {
            log.warn("Category ID validation failed: invalid ID ({})", categoryId);
            throw new BadRequestException("ID danh mục không hợp lệ");
        }
    }

    /**
     * Validates a product ID.
     * 
     * Validation rules:
     * - Product ID must be positive
     * 
     * Requirements: 13.1
     * 
     * @param productId The product ID to validate
     * @throws BadRequestException if validation fails
     */
    public void validateProductId(Long productId) {
        if (productId == null || productId <= 0) {
            log.warn("Product ID validation failed: invalid ID ({})", productId);
            throw new BadRequestException("ID sản phẩm không hợp lệ");
        }
    }

    /**
     * Validates an engagement score threshold.
     * 
     * Validation rules:
     * - Engagement score must be non-negative if provided
     * 
     * Requirements: 13.1
     * 
     * @param minEngagementScore The minimum engagement score (nullable)
     * @throws BadRequestException if validation fails
     */
    public void validateEngagementScore(Integer minEngagementScore) {
        if (minEngagementScore != null && minEngagementScore < 0) {
            log.warn("Engagement score validation failed: negative value ({})", minEngagementScore);
            throw new BadRequestException("Điểm tương tác tối thiểu phải lớn hơn hoặc bằng 0");
        }
    }

    /**
     * Validates a display order for pinned products.
     * 
     * Validation rules:
     * - Display order must be non-negative
     * 
     * Requirements: 13.1
     * 
     * @param displayOrder The display order to validate
     * @throws BadRequestException if validation fails
     */
    public void validateDisplayOrder(int displayOrder) {
        if (displayOrder < 0) {
            log.warn("Display order validation failed: negative value ({})", displayOrder);
            throw new BadRequestException("Thứ tự hiển thị phải lớn hơn hoặc bằng 0");
        }
    }

    /**
     * Validates sort parameters.
     * 
     * Validation rules:
     * - Sort field must not be null or empty
     * - Sort direction must be either "asc" or "desc"
     * 
     * Requirements: 13.1
     * 
     * @param sortBy The field to sort by
     * @param sortDir The sort direction ("asc" or "desc")
     * @throws BadRequestException if validation fails
     */
    public void validateSortParameters(String sortBy, String sortDir) {
        log.debug("Validating sort parameters: sortBy={}, sortDir={}", sortBy, sortDir);
        
        if (sortBy == null || sortBy.trim().isEmpty()) {
            log.warn("Sort validation failed: sortBy is null or empty");
            throw new BadRequestException("Trường sắp xếp không được để trống");
        }
        
        if (sortDir == null || sortDir.trim().isEmpty()) {
            log.warn("Sort validation failed: sortDir is null or empty");
            throw new BadRequestException("Hướng sắp xếp không được để trống");
        }
        
        String normalizedSortDir = sortDir.trim().toLowerCase();
        if (!normalizedSortDir.equals("asc") && !normalizedSortDir.equals("desc")) {
            log.warn("Sort validation failed: invalid sort direction ({})", sortDir);
            throw new BadRequestException("Hướng sắp xếp phải là 'asc' hoặc 'desc'");
        }
        
        log.debug("Sort parameters validated successfully");
    }
}
