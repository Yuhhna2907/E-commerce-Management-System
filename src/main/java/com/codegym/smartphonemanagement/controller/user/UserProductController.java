package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.service.logicDiscount.DiscountService;
import com.codegym.smartphonemanagement.service.product.DTO.ProductResponseDTO;
import com.codegym.smartphonemanagement.service.product.DTO.ReviewRequestDTO;
import com.codegym.smartphonemanagement.service.product.DTO.ReviewResponseDTO;
import com.codegym.smartphonemanagement.service.product.user.IUserProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user/products")
public class UserProductController {

    private final IUserProductService userProductService;
    private final DiscountService discountService;

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
    public String productDetail(@PathVariable Long id, Model model) {
        ProductResponseDTO product = userProductService.getProductById(id);
        List<ReviewResponseDTO> reviews = userProductService.getReviewsByProductId(id);

        Map<Integer, Double> starPercents = new HashMap<>();
        Map<Integer, Long> starCounts = new HashMap<>();

        for (int i = 1; i <= 5; i++) {
            final int star = i;
            long count = reviews.stream().filter(r -> r.getRating() == star).count();
            double percent = (product.getTotalReviews() > 0) ? (count * 100.0 / product.getTotalReviews()) : 0;

            starCounts.put(i, count);
            starPercents.put(i, percent);
        }

        model.addAttribute("product", product);
        model.addAttribute("reviews", reviews);
        model.addAttribute("starPercents", starPercents);
        model.addAttribute("starCounts", starCounts);

        return "user/product/detail";
    }

    @PostMapping("/review")
    @ResponseBody
    public ResponseEntity<ReviewResponseDTO> createReview(
            @RequestBody @Valid ReviewRequestDTO request) {
        Long userId = 1L;

        ReviewResponseDTO response =
                userProductService.reviewProduct(userId,request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/reviews")
    @ResponseBody
    public List<ReviewResponseDTO> getReviews(@PathVariable Long id) {
        return userProductService.getReviewsByProductId(id);
    }

    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<List<ProductResponseDTO>> getProductsForComparison(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Page<ProductResponseDTO> productPage = userProductService.searchProducts(
                keyword,
                null,
                null,
                null,
                page,
                size,
                "asc"
        );
        return ResponseEntity.ok(productPage.getContent());
    }
}