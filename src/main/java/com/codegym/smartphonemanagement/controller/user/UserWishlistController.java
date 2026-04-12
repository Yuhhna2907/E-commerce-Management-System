package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.service.product.user.IUserProductService;
import com.codegym.smartphonemanagement.service.product.DTO.ProductResponseDTO;
import com.codegym.smartphonemanagement.service.wishlist.IWishlistService;
import com.codegym.smartphonemanagement.service.wishlist.DTO.WishlistRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
        List<Product> wishlistProducts = wishlistService.getWishlistProductsByUserId(USER_ID);
        List<ProductResponseDTO> wishlistItems = wishlistProducts.stream()
                .map(p -> userProductService.getProductById(p.getId()))
                .collect(Collectors.toList());
        model.addAttribute("wishlistItems", wishlistItems);
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
