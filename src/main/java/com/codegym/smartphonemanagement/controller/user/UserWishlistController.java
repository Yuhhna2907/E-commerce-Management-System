package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.dto.BreadcrumbItem;
import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.service.product.user.IUserProductService;
import com.codegym.smartphonemanagement.service.product.DTO.ProductResponseDTO;
import com.codegym.smartphonemanagement.service.wishlist.IWishlistService;
import com.codegym.smartphonemanagement.service.wishlist.DTO.WishlistRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class UserWishlistController {

    private final IWishlistService wishlistService;
    private final IUserProductService userProductService;


    private final Long USER_ID = 1L;

    /**
     * Giao diện Xem Bộ sưu tập (Wishlist Dashboard)
     */
    @GetMapping("/user/wishlist")
    public String showWishlistPage(Model model) {
        try {
            List<Product> wishlistProducts = wishlistService.getWishlistProductsByUserId(USER_ID);
            
            // Filter out null products and convert to DTO
            List<ProductResponseDTO> wishlistItems = wishlistProducts.stream()
                    .filter(p -> p != null && p.getId() != null)
                    .map(p -> {
                        try {
                            return userProductService.getProductById(p.getId());
                        } catch (Exception e) {
                            System.err.println("Error converting product " + p.getId() + ": " + e.getMessage());
                            return null;
                        }
                    })
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());
            
            model.addAttribute("wishlistItems", wishlistItems);
            System.out.println("Wishlist size: " + wishlistItems.size());
            
        } catch (Exception e) {
            System.err.println("Error loading wishlist: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("wishlistItems", List.of());
        }
        
        // Add breadcrumb navigation
        List<BreadcrumbItem> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(BreadcrumbItem.builder()
                .label("Trang chủ")
                .url("/user/products")
                .active(false)
                .build());
        breadcrumbs.add(BreadcrumbItem.builder()
                .label("Danh sách yêu thích")
                .url(null)
                .active(true)
                .build());
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "user/wishlist/list";
    }

    /**
     * API AJAX xử lý Thêm/Xoá Tim Wishlist (Micro-Interaction)
     */
    @PostMapping("/api/user/wishlist/toggle")
    @ResponseBody
    public Map<String, Object> toggleWishlist(@RequestBody WishlistRequestDTO requestDTO) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean isAdded = wishlistService.toggleWishlist(USER_ID, requestDTO.getProductId());
            response.put("success", true);
            response.put("isAdded", isAdded);
            response.put("message", isAdded ? "Đã thêm vào bộ sưu tập" : "Đã xoá khỏi bộ sưu tập");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * API AJAX xóa Wishlist (sử dụng trong trang Bộ sưu tập)
     */
    @PostMapping("/api/user/wishlist/remove")
    @ResponseBody
    public Map<String, Object> removeWishlist(@RequestBody WishlistRequestDTO requestDTO) {
        Map<String, Object> response = new HashMap<>();
        try {
            wishlistService.removeWishlistItem(USER_ID, requestDTO.getProductId());
            response.put("success", true);
            response.put("message", "Đã xoá khỏi bộ sưu tập");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
