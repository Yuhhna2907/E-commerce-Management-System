package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.SearchLog;
import com.codegym.smartphonemanagement.model.dto.SearchTrendDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for SearchLog entity
 * Provides queries for search trend analytics
 */
@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
    
    /**
     * Get top N search keywords from last N days
     * Used for Trending Search Widget on dashboard
     */
    @Query("""
        SELECT new com.codegym.smartphonemanagement.model.dto.SearchTrendDTO(
            sl.searchKeyword,
            CAST(COUNT(sl.id) AS int),
            CAST(SUM(CASE WHEN sl.converted = true THEN 1 ELSE 0 END) AS int),
            CAST((SUM(CASE WHEN sl.converted = true THEN 1 ELSE 0 END) * 100.0 / COUNT(sl.id)) AS java.math.BigDecimal),
            'stable',
            MIN(sl.searchTimestamp),
            MAX(sl.searchTimestamp)
        )
        FROM SearchLog sl
        WHERE sl.searchTimestamp >= :startDate
        GROUP BY sl.searchKeyword
        ORDER BY COUNT(sl.id) DESC
    """)
    List<SearchTrendDTO> findTopSearchKeywords(
        @Param("startDate") LocalDateTime startDate,
        Pageable pageable
    );
    
    /**
     * Get paginated search trends with date range filter
     * Used for Search Trends detail page
     */
    @Query("""
        SELECT new com.codegym.smartphonemanagement.model.dto.SearchTrendDTO(
            sl.searchKeyword,
            CAST(COUNT(sl.id) AS int),
            CAST(SUM(CASE WHEN sl.converted = true THEN 1 ELSE 0 END) AS int),
            CAST((SUM(CASE WHEN sl.converted = true THEN 1 ELSE 0 END) * 100.0 / COUNT(sl.id)) AS java.math.BigDecimal),
            'stable',
            MIN(sl.searchTimestamp),
            MAX(sl.searchTimestamp)
        )
        FROM SearchLog sl
        WHERE sl.searchTimestamp BETWEEN :startDate AND :endDate
        GROUP BY sl.searchKeyword
    """)
    Page<SearchTrendDTO> findSearchTrendsData(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    /**
     * Get all search trends for export (no pagination)
     */
    @Query("""
        SELECT new com.codegym.smartphonemanagement.model.dto.SearchTrendDTO(
            sl.searchKeyword,
            CAST(COUNT(sl.id) AS int),
            CAST(SUM(CASE WHEN sl.converted = true THEN 1 ELSE 0 END) AS int),
            CAST((SUM(CASE WHEN sl.converted = true THEN 1 ELSE 0 END) * 100.0 / COUNT(sl.id)) AS java.math.BigDecimal),
            'stable',
            MIN(sl.searchTimestamp),
            MAX(sl.searchTimestamp)
        )
        FROM SearchLog sl
        WHERE sl.searchTimestamp BETWEEN :startDate AND :endDate
        GROUP BY sl.searchKeyword
        ORDER BY COUNT(sl.id) DESC
    """)
    List<SearchTrendDTO> findAllSearchTrendsData(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Find recent search logs for a user (for conversion tracking)
     */
    @Query("""
        SELECT sl FROM SearchLog sl
        WHERE sl.user.id = :userId
        AND sl.converted = false
        AND sl.searchTimestamp >= :since
        ORDER BY sl.searchTimestamp DESC
    """)
    List<SearchLog> findRecentSearchesByUser(
        @Param("userId") Long userId,
        @Param("since") LocalDateTime since
    );
}
