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

        // Giữ lại filter để không mất khi phân trang
        model.addAttribute("keyword", keyword);
        model.addAttribute("brand", brand);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sort", sort);

        return "user/product/list";
    }
}