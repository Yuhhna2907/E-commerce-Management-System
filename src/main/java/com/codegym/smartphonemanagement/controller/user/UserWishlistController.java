package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.dto.BreadcrumbItem;
import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.service.product.user.IUserProductService;
import com.codegym.smartphonemanagement.service.product.DTO.ProductResponseDTO;
import com.codegym.smartphonemanagement.service.wishlist.IWishlistService;
import com.codegym.smartphonemanagement.service.wishlist.DTO.WishlistRequestDTO;
import com.codegym.smartphonemanagement.util.SecurityUtil;
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
    private final UserRepository userRepository;

    // Lấy userId từ SecurityContext
    private Long getCurrentUserId() {
        return SecurityUtil.getCurrentUserId(userRepository);
    }

    /**
     * Giao diện Xem Bộ sưu tập (Wishlist Dashboard)
     */
    @GetMapping("/user/wishlist")
    public String showWishlistPage(Model model) {
        try {
            List<Product> wishlistProducts = wishlistService.getWishlistProductsByUserId(getCurrentUserId());
            System.out.println("=== DEBUG: Loading wishlist for user " + USER_ID + " ===");
            
            List<Product> wishlistProducts = wishlistService.getWishlistProductsByUserId(USER_ID);
            System.out.println("Raw wishlist products count: " + (wishlistProducts != null ? wishlistProducts.size() : "null"));
            
            if (wishlistProducts == null) {
                System.out.println("WARNING: wishlistProducts is null!");
                wishlistProducts = List.of();
            }
            
            // Filter out null products and convert to DTO
            List<ProductResponseDTO> wishlistItems = wishlistProducts.stream()
                    .filter(p -> {
                        if (p == null) {
                            System.out.println("WARNING: Found null product in wishlist");
                            return false;
                        }
                        if (p.getId() == null) {
                            System.out.println("WARNING: Found product with null ID: " + p);
                            return false;
                        }
                        return true;
                    })
                    .map(p -> {
                        try {
                            System.out.println("Converting product: " + p.getId() + " - " + p.getName());
                            ProductResponseDTO dto = userProductService.getProductById(p.getId());
                            System.out.println("Converted successfully: " + dto.getName());
                            return dto;
                        } catch (Exception e) {
                            System.err.println("Error converting product " + p.getId() + ": " + e.getMessage());
                            e.printStackTrace();
                            return null;
                        }
                    })
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());
            
            System.out.println("Final wishlist items count: " + wishlistItems.size());
            model.addAttribute("wishlistItems", wishlistItems);
            
        } catch (Exception e) {
            System.err.println("ERROR loading wishlist: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("wishlistItems", List.of());
            model.addAttribute("errorMessage", "Không thể tải danh sách yêu thích: " + e.getMessage());
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
            boolean isAdded = wishlistService.toggleWishlist(getCurrentUserId(), requestDTO.getProductId());
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
            wishlistService.removeWishlistItem(getCurrentUserId(), requestDTO.getProductId());
            response.put("success", true);
            response.put("message", "Đã xoá khỏi bộ sưu tập");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
