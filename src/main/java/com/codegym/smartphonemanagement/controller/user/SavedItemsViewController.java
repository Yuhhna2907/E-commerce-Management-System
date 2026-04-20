package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.model.dto.SavedForLaterDTO;
import com.codegym.smartphonemanagement.service.savedforlater.SavedForLaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class SavedItemsViewController {
    
    private final SavedForLaterService savedForLaterService;
    
    private static final Long USER_ID = 1L; // TODO: Get from Security context
    
    /**
     * Hiển thị trang Saved Items (HTML view)
     * Maps to /user/saved-items
     */
    @GetMapping("/saved-items")
    public String showSavedItemsPage(Model model) {
        List<SavedForLaterDTO> savedItems = savedForLaterService.getSavedItems(USER_ID);
        
        model.addAttribute("savedItems", savedItems);
        model.addAttribute("savedCount", savedItems.size());
        
        // Add breadcrumb navigation (following pattern from other controllers)
        List<com.codegym.smartphonemanagement.dto.BreadcrumbItem> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(com.codegym.smartphonemanagement.dto.BreadcrumbItem.builder()
                .label("Trang chủ")
                .url("/user/products")
                .active(false)
                .build());
        breadcrumbs.add(com.codegym.smartphonemanagement.dto.BreadcrumbItem.builder()
                .label("Sản phẩm đã lưu")
                .url(null)
                .active(true)
                .build());
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "user/saved/list";
    }
}
