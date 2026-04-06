package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class UserProductController {

    private final ProductRepository productRepository;

    @GetMapping("/user/products")
    public String viewProducts(Model model) {
        model.addAttribute("products", productRepository.findAll());
        return "user/product/list";
    }
}