package com.codegym.smartphonemanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a single item in the breadcrumb navigation.
 * Used to display hierarchical navigation path across pages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreadcrumbItem {
    
    /**
     * Display text for the breadcrumb item (e.g., "Trang chủ", "Sản phẩm")
     */
    private String label;
    
    /**
     * URL to navigate to when clicking the breadcrumb item.
     * Null for the current page (active item).
     */
    private String url;
    
    /**
     * Whether this is the current/active page.
     * Active items are not clickable.
     */
    private boolean active;
}
