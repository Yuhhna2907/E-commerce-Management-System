package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.service.product.DTO.ProductResponseDTO;
import com.codegym.smartphonemanagement.service.product.user.IUserProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user/products")
public class UserProductController {

    private final IUserProductService userProductService;

    @GetMapping
    public String listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "asc") String sort,
            Model model
    ) {

        Page<ProductResponseDTO> productPage = userProductService.searchProducts(
                keyword,
                brand,
                minPrice,
                maxPrice,
                page,
                size,
                sort
        );

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());

        // Giữ lại filter để không mất khi phân trang
        model.addAttribute("keyword", keyword);
        model.addAttribute("brand", brand);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sort", sort);

        return "user/product/list";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ProductResponseDTO getProductDetail(@PathVariable Long id) {
        return userProductService.getProductById(id);
    }
}