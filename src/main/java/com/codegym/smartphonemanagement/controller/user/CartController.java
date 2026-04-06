package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.model.Cart;
import com.codegym.smartphonemanagement.service.cart.DTO.CartItemRequestDTO;
import com.codegym.smartphonemanagement.service.cart.DTO.CartResponseDTO;
import com.codegym.smartphonemanagement.service.cart.user.ICartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user/cart")
public class CartController {

    private final ICartService cartService;

    // ⚠️ Demo: hardcode userId (sau này thay bằng Security)
    private final Long USER_ID = 1L;

    // Hiển thị giỏ hàng
    @GetMapping
    public String viewCart(HttpSession session, Model model) {

        CartResponseDTO cartResponse = cartService.getCart(USER_ID);

        if (cart == null) {
            cart = new Cart();
        }

        model.addAttribute("cart", cart);

        return "user/cart/list"; // file HTML
    }

    // Thêm sản phẩm vào giỏ
    @PostMapping("/add")
    public String addToCart(@ModelAttribute CartItemRequestDTO request) {
        cartService.addToCart(USER_ID, request);
        return "redirect:/user/cart";
    }

    // Update số lượng
    @PostMapping("/update")
    public String updateCart(@ModelAttribute CartItemRequestDTO request) {
        cartService.updateQuantity(USER_ID, request);
        return "redirect:/user/cart";
    }

    // Xoá item
    @GetMapping("/remove/{productId}")
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