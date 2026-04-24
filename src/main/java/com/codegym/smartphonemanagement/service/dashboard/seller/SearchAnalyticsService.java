package com.codegym.smartphonemanagement.service.dashboard.seller;

import com.codegym.smartphonemanagement.model.dto.SearchTrendDTO;
import com.codegym.smartphonemanagement.repository.user.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("dashboardSearchAnalyticsService")
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class SearchAnalyticsService {

    private final SearchLogRepository searchLogRepository;

    /**
     * Lấy dữ liệu xu hướng tìm kiếm cho Dashboard
     */
    public Map<String, Object> getSearchAnalyticsSummary(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        Pageable top10 = PageRequest.of(0, 10);
        
        List<SearchTrendDTO> topTrends = searchLogRepository.findTopSearchKeywords(startDate, top10);
        
        // Tính toán các chỉ số tổng quan
        long totalSearches = topTrends.stream().mapToLong(SearchTrendDTO::getSearchCount).sum();
        long totalConversions = topTrends.stream().mapToLong(SearchTrendDTO::getConversionCount).sum();
        
        BigDecimal avgConversionRate = totalSearches > 0 
            ? BigDecimal.valueOf(totalConversions * 100.0 / totalSearches).setScale(2, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        Map<String, Object> summary = new HashMap<>();
        summary.put("topTrends", topTrends);
        summary.put("totalSearches", totalSearches);
        summary.put("totalConversions", totalConversions);
        summary.put("avgConversionRate", avgConversionRate);
        summary.put("periodDays", days);
        
        return summary;
    }

    /**
     * Lấy dữ liệu chi tiết có phân trang cho trang Analytics
     */
    public Page<SearchTrendDTO> getDetailedSearchTrends(LocalDateTime start, LocalDateTime end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return searchLogRepository.findSearchTrendsData(start, end, pageable);
    }
}
