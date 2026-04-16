package com.codegym.smartphonemanagement.controller.api;

import com.codegym.smartphonemanagement.service.cart.DTO.CartResponseDTO;
import com.codegym.smartphonemanagement.service.cart.user.ICartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API Controller for Cart operations
 * 
 * Provides API endpoints for AJAX cart operations including:
 * - Cart item count for bottom navigation badge
 * - Cart totals for UI updates after deletions
 * 
 * Used by mobile components like BottomNavigation and SwipeToDelete
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
public class CartApiController {
    
    private final ICartService cartService;
    
    // Demo: hardcode userId (should use Security in production)
    private final Long USER_ID = 1L;
    
    /**
     * Get cart item count for bottom navigation badge
     * 
     * @return JSON response with cart item count
     */
    @GetMapping("/count")
    public Map<String, Object> getCartCount() {
        Map<String, Object> response = new HashMap<>();
        try {
            CartResponseDTO cartResponse = cartService.getCart(USER_ID);
            response.put("success", true);
            response.put("count", cartResponse.getItems().size());
        } catch (Exception e) {
            response.put("success", false);
            response.put("count", 0);
            response.put("message", e.getMessage());
        }
        return response;
    }
}