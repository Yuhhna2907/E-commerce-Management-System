package com.codegym.smartphonemanagement.service.analytics;

import com.codegym.smartphonemanagement.model.dto.SearchTrendDTO;
import com.codegym.smartphonemanagement.repository.user.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Service for Search Trend Analytics
 * Provides insights into user search behavior and conversion rates
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SearchAnalyticsService {
    
    private final SearchLogRepository searchLogRepository;
    
    /**
     * Calculate conversion rate as percentage
     * Formula: (conversionCount / searchCount) * 100
     * 
     * @param conversionCount Number of searches that led to purchase
     * @param searchCount Total number of searches
     * @return Conversion rate as percentage with 2 decimal places
     */
    public BigDecimal calculateConversionRate(int conversionCount, int searchCount) {
        if (searchCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(conversionCount)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(searchCount), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Get top 10 search trends from last 7 days for dashboard widget
     * Results are cached for 5 minutes to reduce database load
     * 
     * @return List of top 10 search keywords by frequency
     */
    @Cacheable(value = "searchWidget", key = "'top10'")
    public List<SearchTrendDTO> getTop10SearchTrends() {
        log.debug("Fetching top 10 search trends from last 7 days");
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        Pageable pageable = PageRequest.of(0, 10);
        
        List<SearchTrendDTO> results = searchLogRepository.findTopSearchKeywords(startDate, pageable);
        
        // If no search data, return sample trending searches
        if (results.isEmpty()) {
            log.warn("No search data found, returning sample trending searches");
            results = getSampleTrendingSearches();
        }
        
        log.info("Found {} search trends", results.size());
        return results;
    }
    
    /**
     * Get sample trending searches when no real data exists
     */
    private List<SearchTrendDTO> getSampleTrendingSearches() {
        String[] sampleKeywords = {
            "iPhone 15 Pro Max", "Samsung Galaxy S24", "Xiaomi 14", 
            "OPPO Find X7", "Realme GT 5", "Vivo X100",
            "iPhone 14", "Samsung A54", "Redmi Note 13", "OPPO Reno 11"
        };
        
        return java.util.stream.IntStream.range(0, sampleKeywords.length)
            .mapToObj(i -> SearchTrendDTO.builder()
                .searchKeyword(sampleKeywords[i])
                .searchCount(100 - (i * 8))
                .conversionCount(0)
                .conversionRate(BigDecimal.ZERO)
                .trendIndicator("stable")
                .firstSearchedDate(LocalDateTime.now().minusDays(7))
                .lastSearchedDate(LocalDateTime.now())
                .build())
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get paginated search trends data with date range filtering
     * Used for the detail analytics page
     * 
     * @param startDate Start of date range (defaults to 7 days ago if null)
     * @param endDate End of date range (defaults to now if null)
     * @param sortBy Field to sort by
     * @param sortDir Sort direction (asc or desc)
     * @param pageable Pagination parameters
     * @return Page of search trend data
     */
    public Page<SearchTrendDTO> getSearchTrendsData(
            LocalDate startDate,
            LocalDate endDate,
            String sortBy,
            String sortDir,
            Pageable pageable) {
        
        // Default to last 7 days if not specified
        LocalDateTime startDateTime = (startDate != null) 
            ? startDate.atStartOfDay() 
            : LocalDateTime.now().minusDays(7);
        
        LocalDateTime endDateTime = (endDate != null) 
            ? endDate.atTime(LocalTime.MAX) 
            : LocalDateTime.now();
        
        log.debug("Fetching search trends data - start: {}, end: {}, sortBy: {}, sortDir: {}", 
                  startDateTime, endDateTime, sortBy, sortDir);
        
        // Create pageable with dynamic sorting
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable sortedPageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            sort
        );
        
        Page<SearchTrendDTO> results = searchLogRepository
            .findSearchTrendsData(startDateTime, endDateTime, sortedPageable);
        
        log.info("Found {} search trend records (page {} of {})", 
                 results.getNumberOfElements(), 
                 results.getNumber() + 1, 
                 results.getTotalPages());
        
        return results;
    }
    
    /**
     * Get all search trends data for export (no pagination)
     * 
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of all matching search trend data
     */
    public List<SearchTrendDTO> getAllSearchTrendsData(LocalDate startDate, LocalDate endDate) {
        // Default to last 7 days if not specified
        LocalDateTime startDateTime = (startDate != null) 
            ? startDate.atStartOfDay() 
            : LocalDateTime.now().minusDays(7);
        
        LocalDateTime endDateTime = (endDate != null) 
            ? endDate.atTime(LocalTime.MAX) 
            : LocalDateTime.now();
        
        log.debug("Fetching all search trends data for export - start: {}, end: {}", 
                  startDateTime, endDateTime);
        
        List<SearchTrendDTO> results = searchLogRepository
            .findAllSearchTrendsData(startDateTime, endDateTime);
        
        log.info("Exporting {} search trend records", results.size());
        return results;
    }
}
