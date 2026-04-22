package com.codegym.smartphonemanagement.service.analytics;

import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.model.dto.ProductEngagementDTO;
import com.codegym.smartphonemanagement.repository.seller.ProductRepository;
import com.codegym.smartphonemanagement.repository.user.CompareRepository;
import com.codegym.smartphonemanagement.repository.user.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Marketing Analytics
 * Provides insights into product engagement (wishlist + compare actions)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MarketingAnalyticsService {
    
    private final WishlistRepository wishlistRepository;
    private final CompareRepository compareRepository;
    private final ProductRepository productRepository;
    
    /**
     * Calculate engagement score for a product
     * Formula: (wishlistCount * 2) + (compareCount * 1)
     * Wishlist is weighted 2x because it indicates stronger purchase intent
     * 
     * @param wishlistCount Number of times product was added to wishlist
     * @param compareCount Number of times product was added to compare
     * @return Calculated engagement score
     */
    public int calculateEngagementScore(int wishlistCount, int compareCount) {
        return (wishlistCount * 2) + (compareCount * 1);
    }
    
    /**
     * Get top 5 products with highest engagement for dashboard widget
     * Results are cached for 5 minutes to reduce database load
     * 
     * @return List of top 5 products by engagement score
     */
    @Cacheable(value = "marketingWidget", key = "'top5'")
    public List<ProductEngagementDTO> getTop5ProductEngagement() {
        log.debug("Fetching top 5 product engagement");
        
        // Get all engagement data and sort by score
        List<ProductEngagementDTO> allData = buildEngagementData(null, null);
        
        // If no engagement data, return top 5 products with mock engagement
        if (allData.isEmpty()) {
            log.warn("No engagement data found, returning top 5 active products with simulated engagement");
            return getTop5ProductsWithSimulatedEngagement();
        }
        
        // Return top 5
        List<ProductEngagementDTO> top5 = allData.stream()
            .sorted(Comparator.comparingInt(ProductEngagementDTO::getEngagementScore).reversed())
            .limit(5)
            .collect(Collectors.toList());
        
        log.info("Found {} products with engagement data, returning top 5", allData.size());
        return top5;
    }
    
    /**
     * Get top 5 products with simulated engagement when no real data exists
     * This provides a better UX than showing empty widgets
     */
    private List<ProductEngagementDTO> getTop5ProductsWithSimulatedEngagement() {
        List<Product> topProducts = productRepository.findTop5ByActiveOrderByCreatedAtDesc(true);
        
        return topProducts.stream()
            .map(product -> ProductEngagementDTO.builder()
                .productId(product.getId())
                .productName(product.getName())
                .imageUrl(product.getImageUrl())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : "N/A")
                .wishlistCount(0)
                .compareCount(0)
                .engagementScore(0)
                .lastUpdated(LocalDateTime.now())
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * Get paginated product engagement data with filtering and sorting
     * Used for the detail analytics page
     * 
     * @param categoryId Optional category filter
     * @param minEngagementScore Optional minimum engagement score filter
     * @param sortBy Field to sort by
     * @param sortDir Sort direction (asc or desc)
     * @param pageable Pagination parameters
     * @return Page of product engagement data
     */
    public Page<ProductEngagementDTO> getProductEngagementData(
            Long categoryId,
            Integer minEngagementScore,
            String sortBy,
            String sortDir,
            Pageable pageable) {
        
        log.debug("Fetching product engagement data - categoryId: {}, minScore: {}, sortBy: {}, sortDir: {}", 
                  categoryId, minEngagementScore, sortBy, sortDir);
        
        // Build engagement data with filters
        List<ProductEngagementDTO> allData = buildEngagementData(categoryId, minEngagementScore);
        
        // Sort data
        Comparator<ProductEngagementDTO> comparator = getComparator(sortBy, sortDir);
        allData.sort(comparator);
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allData.size());
        List<ProductEngagementDTO> pageContent = allData.subList(start, end);
        
        Page<ProductEngagementDTO> result = new PageImpl<>(pageContent, pageable, allData.size());
        
        log.info("Found {} product engagement records (page {} of {})", 
                 result.getNumberOfElements(), 
                 result.getNumber() + 1, 
                 result.getTotalPages());
        
        return result;
    }
    
    /**
     * Get all product engagement data for export (no pagination)
     * 
     * @param categoryId Optional category filter
     * @param minEngagementScore Optional minimum engagement score filter
     * @return List of all matching product engagement data
     */
    public List<ProductEngagementDTO> getAllProductEngagementData(
            Long categoryId,
            Integer minEngagementScore) {
        
        log.debug("Fetching all product engagement data for export - categoryId: {}, minScore: {}", 
                  categoryId, minEngagementScore);
        
        List<ProductEngagementDTO> results = buildEngagementData(categoryId, minEngagementScore);
        
        // Sort by engagement score descending
        results.sort(Comparator.comparingInt(ProductEngagementDTO::getEngagementScore).reversed());
        
        log.info("Exporting {} product engagement records", results.size());
        return results;
    }
    
    /**
     * Build engagement data by combining wishlist and compare counts
     * 
     * @param categoryId Optional category filter
     * @param minEngagementScore Optional minimum score filter
     * @return List of product engagement DTOs
     */
    private List<ProductEngagementDTO> buildEngagementData(Long categoryId, Integer minEngagementScore) {
        // Get wishlist counts per product
        Map<Long, Integer> wishlistCounts = new HashMap<>();
        wishlistRepository.countByProduct().forEach(row -> {
            Long productId = (Long) row[0];
            Long count = (Long) row[1];
            wishlistCounts.put(productId, count.intValue());
        });
        
        // Get compare counts per product
        Map<Long, Integer> compareCounts = new HashMap<>();
        compareRepository.countByProduct().forEach(row -> {
            Long productId = (Long) row[0];
            Long count = (Long) row[1];
            compareCounts.put(productId, count.intValue());
        });
        
        // Get all product IDs that have engagement
        Set<Long> productIds = new HashSet<>();
        productIds.addAll(wishlistCounts.keySet());
        productIds.addAll(compareCounts.keySet());
        
        // Build DTOs
        List<ProductEngagementDTO> results = new ArrayList<>();
        
        for (Long productId : productIds) {
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty() || !productOpt.get().getActive()) {
                continue;
            }
            
            Product product = productOpt.get();
            
            // Apply category filter
            if (categoryId != null && (product.getCategory() == null || 
                !product.getCategory().getId().equals(categoryId))) {
                continue;
            }
            
            int wishlistCount = wishlistCounts.getOrDefault(productId, 0);
            int compareCount = compareCounts.getOrDefault(productId, 0);
            int engagementScore = calculateEngagementScore(wishlistCount, compareCount);
            
            // Apply minimum score filter
            if (minEngagementScore != null && engagementScore < minEngagementScore) {
                continue;
            }
            
            ProductEngagementDTO dto = ProductEngagementDTO.builder()
                .productId(product.getId())
                .productName(product.getName())
                .imageUrl(product.getImageUrl())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : "N/A")
                .wishlistCount(wishlistCount)
                .compareCount(compareCount)
                .engagementScore(engagementScore)
                .lastUpdated(LocalDateTime.now())
                .build();
            
            results.add(dto);
        }
        
        return results;
    }
    
    /**
     * Get comparator for sorting engagement data
     */
    private Comparator<ProductEngagementDTO> getComparator(String sortBy, String sortDir) {
        Comparator<ProductEngagementDTO> comparator;
        
        switch (sortBy) {
            case "wishlistCount":
                comparator = Comparator.comparingInt(ProductEngagementDTO::getWishlistCount);
                break;
            case "compareCount":
                comparator = Comparator.comparingInt(ProductEngagementDTO::getCompareCount);
                break;
            case "productName":
                comparator = Comparator.comparing(ProductEngagementDTO::getProductName);
                break;
            case "engagementScore":
            default:
                comparator = Comparator.comparingInt(ProductEngagementDTO::getEngagementScore);
                break;
        }
        
        if ("asc".equalsIgnoreCase(sortDir)) {
            return comparator;
        } else {
            return comparator.reversed();
        }
    }
}
