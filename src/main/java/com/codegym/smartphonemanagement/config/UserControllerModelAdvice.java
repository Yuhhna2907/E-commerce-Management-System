package com.codegym.smartphonemanagement.config;

import com.codegym.smartphonemanagement.service.cart.user.ICartService;
import com.codegym.smartphonemanagement.service.order.user.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.codegym.smartphonemanagement.controller.user")
@RequiredArgsConstructor
public class UserControllerModelAdvice {

    private static final Long DEMO_USER_ID = 1L;

    private final ICartService cartService;
    private final IOrderService orderService;

    @ModelAttribute
    public void addUserNavModel(Model model) {
        try {
            model.addAttribute("cart", cartService.getCart(DEMO_USER_ID));
        } catch (Exception e) {
            model.addAttribute("cart", null);
        }
        try {
            model.addAttribute("headerPendingOrdersCount", orderService.countInProgressOrdersByUser(DEMO_USER_ID));
        } catch (Exception e) {
            model.addAttribute("headerPendingOrdersCount", 0L);
        }
    }
}
