package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.repository.seller.ProductRepository;
import com.codegym.smartphonemanagement.service.cart.DTO.CartItemRequestDTO;
import com.codegym.smartphonemanagement.service.cart.DTO.CartResponseDTO;
import com.codegym.smartphonemanagement.service.cart.user.ICartService;
import com.codegym.smartphonemanagement.service.logicDiscount.DiscountService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user/cart")
public class CartController {

    private final ICartService cartService;
    private final ProductRepository productRepository;
    private final DiscountService discountService;

    // ⚠️ Demo: hardcode userId (sau này thay bằng Security)
    private final Long USER_ID = 1L;

    // Hiển thị giỏ hàng
    @GetMapping
    public String viewCart(HttpSession session, Model model) {

        CartResponseDTO cartResponse = cartService.getCart(USER_ID);

        model.addAttribute("cart", cartResponse);

        return "user/cart/list"; // file HTML
    }

    @PostMapping("/add")
    @ResponseBody
    public Map<String, Object> addToCart(@RequestBody CartItemRequestDTO request) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (request == null || request.getProductId() == null) {
                response.put("success", false);
                response.put("message", "Dữ liệu gửi lên không hợp lệ!");
                return response;
            }

            Optional<Product> productOpt = productRepository.findById(request.getProductId());

            if (productOpt.isPresent()) {
                Product product = productOpt.get();

                // Thực hiện thêm vào giỏ hàng (giữ từ nhánh develop)
                cartService.addToCart(USER_ID, request);

                response.put("success", true);
                response.put("message", "Thêm thành công!");

                // Thông tin cơ bản
                response.put("productName", product.getName());
                response.put("productPrice", product.getPrice());
                response.put("productImage", product.getImageUrl());

                // Thông tin chi tiết (giữ từ nhánh develop)
                response.put("brand", product.getBrand());
                response.put("color", product.getColor());
                response.put("storage", product.getStorage());

                // Logic giảm giá của bạn (thêm vào)
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
    public String updateCart(@ModelAttribute CartItemRequestDTO request) {
        cartService.updateQuantity(USER_ID, request);
        return "redirect:/user/cart";
    }

    // Xoá item
    @PostMapping("/remove/{productId}")
    public String removeItem(@PathVariable Long productId) {
        cartService.removeItem(USER_ID, productId);
        return "redirect:/user/cart";
    }

    // Clear cart
    @PostMapping("/clear")
    public String clearCart() {
        cartService.clearCart(USER_ID);
        return "redirect:/user/cart";
    }
}