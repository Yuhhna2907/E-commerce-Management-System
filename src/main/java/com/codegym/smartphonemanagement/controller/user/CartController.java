package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.dto.BreadcrumbItem;
import com.codegym.smartphonemanagement.exception.BadRequestException;
import com.codegym.smartphonemanagement.exception.ResourceNotFoundException;
import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.model.ProductVariant;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.model.dto.SavedForLaterDTO;
import com.codegym.smartphonemanagement.repository.seller.ProductRepository;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.service.cart.DTO.CartItemRequestDTO;
import com.codegym.smartphonemanagement.service.cart.DTO.CartResponseDTO;
import com.codegym.smartphonemanagement.service.cart.user.ICartService;
import com.codegym.smartphonemanagement.service.logicDiscount.DiscountService;
import com.codegym.smartphonemanagement.util.SecurityUtil;
import com.codegym.smartphonemanagement.service.savedforlater.SavedForLaterService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user/cart")
public class CartController {

    private final ICartService cartService;
    private final ProductRepository productRepository;
    private final DiscountService discountService;
    private final UserRepository userRepository;
    private final SavedForLaterService savedForLaterService;

    // Lấy userId từ SecurityContext (đăng nhập)
    private Long getCurrentUserId() {
        return SecurityUtil.getCurrentUserId(userRepository);
    }

    // Hiển thị giỏ hàng
    @GetMapping
    public String viewCart(HttpSession session, Model model) {

        CartResponseDTO cartResponse = cartService.getCart(getCurrentUserId());

        String appliedCoupon = (String) session.getAttribute("APPLIED_COUPON");
        java.math.BigDecimal discountAmt = (java.math.BigDecimal) session.getAttribute("DISCOUNT_AMT");
        
        if (appliedCoupon != null) {
            model.addAttribute("appliedCouponCode", appliedCoupon);
            model.addAttribute("discountAmt", discountAmt);
            
            java.math.BigDecimal currentTotal = cartResponse.getTotalPrice().subtract(discountAmt);
            if (currentTotal.compareTo(java.math.BigDecimal.ZERO) < 0) {
                 currentTotal = java.math.BigDecimal.ZERO;
            }
            model.addAttribute("totalAfterDiscount", currentTotal);
            model.addAttribute("cartRawTotal", cartResponse.getTotalPrice());
        } else {
            model.addAttribute("totalAfterDiscount", cartResponse.getTotalPrice());
            model.addAttribute("cartRawTotal", cartResponse.getTotalPrice());
            model.addAttribute("discountAmt", java.math.BigDecimal.ZERO);
        }

        model.addAttribute("cart", cartResponse);

        // Add breadcrumb navigation
        List<BreadcrumbItem> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(BreadcrumbItem.builder()
                .label("Trang chủ")
                .url("/user/products")
                .active(false)
                .build());
        breadcrumbs.add(BreadcrumbItem.builder()
                .label("Giỏ hàng")
                .url(null)
                .active(true)
                .build());
        model.addAttribute("breadcrumbs", breadcrumbs);

        return "user/cart/list"; // file HTML
    }

    @PostMapping("/add")
    @ResponseBody
    public Map<String, Object> addToCart(@RequestBody CartItemRequestDTO request) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (request == null || request.getProductId() == null || request.getVariantId() == null) {
                response.put("success", false);
                response.put("message", "Dữ liệu gửi lên không hợp lệ!");
                return response;
            }

            Optional<Product> productOpt = productRepository.findById(request.getProductId());

            if (productOpt.isPresent()) {
                Product product = productOpt.get();

                // Lấy biến thể theo variantId
                Optional<ProductVariant> variantOpt = product.getVariants()
                        .stream()
                        .filter(v -> v.getVariantId().equals(request.getVariantId()))
                        .findFirst();

                if (variantOpt.isEmpty()) {
                    response.put("success", false);
                    response.put("message", "Không tìm thấy biến thể sản phẩm!");
                    return response;
                }

                ProductVariant variant = variantOpt.get();

                // Thực hiện thêm vào giỏ hàng
                cartService.addToCart(getCurrentUserId(), request);

                response.put("success", true);
                response.put("message", "Thêm thành công!");

                // Thông tin cơ bản
                response.put("productName", product.getName());
                response.put("productImage", product.getImageUrl());

                // Thông tin chi tiết từ biến thể
                response.put("brand", product.getBrand());
                response.put("color", variant.getColor());
                response.put("storage", variant.getStorage());
                response.put("ram", variant.getRam());
                response.put("productPrice", variant.getSalePrice());
                response.put("stockQuantity", variant.getStockQuantity());

                // Logic giảm giá
                response.put("discountPrice", discountService.applyDiscount(product));
                response.put("discountLabel", discountService.getDiscountLabel(product));

            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy sản phẩm ID: " + request.getProductId());
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi Server: " + e.getMessage());
        }
        return response;
    }

    // Update số lượng
    @PostMapping("/update")
    public String updateCart(@ModelAttribute CartItemRequestDTO request, RedirectAttributes redirectAttributes) {
        try {
            cartService.updateQuantity(getCurrentUserId(), request);
        } catch (BadRequestException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/user/cart";
    }

    // Xoá item
    @PostMapping("/remove/{cartItemId}")
    public String removeItem(@PathVariable Long cartItemId) {
        cartService.removeItem(getCurrentUserId(), cartItemId);
        return "redirect:/user/cart";
    }

    // Clear cart
    @PostMapping("/clear")
    public String clearCart() {
        cartService.clearCart(getCurrentUserId());
        return "redirect:/user/cart";
    }
    
    // API endpoint to get cart totals for AJAX updates
    @GetMapping("/api/totals")
    @ResponseBody
    public Map<String, Object> getCartTotals(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            CartResponseDTO cartResponse = cartService.getCart(USER_ID);
            
            // Calculate total after discount
            java.math.BigDecimal discountAmt = (java.math.BigDecimal) session.getAttribute("DISCOUNT_AMT");
            if (discountAmt == null) {
                discountAmt = java.math.BigDecimal.ZERO;
            }
            
            java.math.BigDecimal totalAfterDiscount = cartResponse.getTotalPrice().subtract(discountAmt);
            if (totalAfterDiscount.compareTo(java.math.BigDecimal.ZERO) < 0) {
                totalAfterDiscount = java.math.BigDecimal.ZERO;
            }
            
            response.put("success", true);
            response.put("itemCount", cartResponse.getItems().size());
            response.put("total", totalAfterDiscount);
            response.put("rawTotal", cartResponse.getTotalPrice());
            response.put("discountAmount", discountAmt);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("itemCount", 0);
            response.put("total", java.math.BigDecimal.ZERO);
        }
        
        return response;
    }
    
    // Save for Later - Move cart item to saved_for_later table
    @PostMapping("/save-for-later")
    @ResponseBody
   
    public Map<String, Object> saveForLater(@RequestParam Long cartItemId) {
        Map<String, Object> response = new HashMap<>();
        try {
            SavedForLaterDTO savedItem = savedForLaterService.saveForLater(USER_ID, cartItemId);
            
            response.put("success", true);
            response.put("message", "Đã lưu sản phẩm để mua sau");
            response.put("data", savedItem);
            response.put("savedCount", savedForLaterService.countSavedItems(USER_ID));
            response.put("cartItemCount", cartService.getCart(USER_ID).getItems().size());
            
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return response;
        }
    }
}