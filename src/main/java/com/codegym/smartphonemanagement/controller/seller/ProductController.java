package com.codegym.smartphonemanagement.controller.seller;

import com.codegym.smartphonemanagement.repository.CategoryRepository;
import com.codegym.smartphonemanagement.service.product.DTO.ProductRequestDTO;
import com.codegym.smartphonemanagement.service.product.DTO.ProductResponseDTO;
import com.codegym.smartphonemanagement.service.product.seller.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final CategoryRepository categoryRepository;

    // ===============================
    // 1. LIST + SEARCH + FILTER + PAGINATION
    // ===============================
    @GetMapping
    public String listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            Model model
    ) {
        // Tạo Sort object dựa trên sort + direction
        Sort sortObj = Sort.by("id"); // mặc định
        if (sort != null && direction != null) {
            if (direction.equalsIgnoreCase("asc")) {
                sortObj = Sort.by(sort).ascending();
            } else {
                sortObj = Sort.by(sort).descending();
            }
        }

        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<ProductResponseDTO> productPage =
                productService.search(keyword, categoryId, page, size, sort, direction);

        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("productRequestDTO", new ProductRequestDTO());

        return "admin/product/list"; // thymeleaf template
    }

    // ===============================
    // 3. SAVE PRODUCT
    // ===============================
    @PostMapping("/save")
    public String saveProduct(@ModelAttribute ProductRequestDTO dto) {
        productService.create(dto);
        return "redirect:/admin/products";
    }


    // ===============================
    // 5. DELETE
    // ===============================
    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return "redirect:/admin/products";
    }
}

