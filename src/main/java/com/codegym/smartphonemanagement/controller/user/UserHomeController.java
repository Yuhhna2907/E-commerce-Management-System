package com.codegym.smartphonemanagement.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserHomeController {

    @GetMapping("/home")
    public String userHome() {
        // Redirect trang home về products
        return "redirect:/user/products";
    }

    @GetMapping("")
    public String userIndex() {
        return "redirect:/user/products";
    }
}
