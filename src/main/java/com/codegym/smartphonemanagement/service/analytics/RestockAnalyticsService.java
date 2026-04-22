package com.codegym.smartphonemanagement.service.analytics;

import com.codegym.smartphonemanagement.model.dto.RestockDemandDTO;
import com.codegym.smartphonemanagement.repository.user.StockNotificationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for Restock Demand Analytics
 * Provides insights into out-of-stock products with customer demand
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RestockAnalyticsService {
    
    private final StockNotificationRequestRepository stockNotificationRequestRepository;
    
    /**
     * Get top 5 products with highest restock demand for dashboard widget
     * Results are cached for 5 minutes to reduce database load
     * 
     * @return List of top 5 out-of-stock products by notification count
     */
    @Cacheable(value = "restockWidget", key = "'top5'")
    public List<RestockDemandDTO> getTop5RestockDemand() {
        log.debug("Fetching top 5 restock demand products");
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "notificationCount"));
        List<RestockDemandDTO> results = stockNotificationRequestRepository.findTop5ByNotificationCount(pageable);
        
        // If no restock demand data, return empty list (this is expected when no products are out of stock)
        if (results.isEmpty()) {
            log.info("No products with restock demand (all products in stock)");
        } else {
            log.info("Found {} products with restock demand", results.size());
        }
        
        return results;
    }
    
    /**
     * Get paginated restock demand data with filtering and sorting
     * Used for the detail analytics page
     * 
     * @param categoryId Optional category filter
     * @param sortBy Field to sort by (notificationCount, productName, etc.)
     * @param sortDir Sort direction (asc or desc)
     * @param pageable Pagination parameters
     * @return Page of restock demand data
     */
    public Page<RestockDemandDTO> getRestockDemandData(
            Long categoryId,
            String sortBy,
            String sortDir,
            Pageable pageable) {
        
        log.debug("Fetching restock demand data - categoryId: {}, sortBy: {}, sortDir: {}, page: {}", 
                  categoryId, sortBy, sortDir, pageable.getPageNumber());
        
        // Create pageable with dynamic sorting
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable sortedPageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            sort
        );
        
        Page<RestockDemandDTO> results = stockNotificationRequestRepository
            .findRestockDemandData(categoryId, sortedPageable);
        
        log.info("Found {} restock demand records (page {} of {})", 
                 results.getNumberOfElements(), 
                 results.getNumber() + 1, 
                 results.getTotalPages());
        
        return results;
    }
    
    /**
     * Get all restock demand data for export (no pagination)
     * 
     * @param categoryId Optional category filter
     * @return List of all matching restock demand data
     */
    public List<RestockDemandDTO> getAllRestockDemandData(Long categoryId) {
        log.debug("Fetching all restock demand data for export - categoryId: {}", categoryId);
        List<RestockDemandDTO> results = stockNotificationRequestRepository
            .findAllRestockDemandData(categoryId);
        log.info("Exporting {} restock demand records", results.size());
        return results;
    }
}
