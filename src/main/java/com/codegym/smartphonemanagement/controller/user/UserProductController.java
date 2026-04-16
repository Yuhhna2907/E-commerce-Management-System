package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.dto.BreadcrumbItem;
import com.codegym.smartphonemanagement.service.logicDiscount.DiscountService;
import com.codegym.smartphonemanagement.service.product.DTO.ProductResponseDTO;
import com.codegym.smartphonemanagement.service.product.DTO.ReviewRequestDTO;
import com.codegym.smartphonemanagement.service.product.DTO.ReviewResponseDTO;
import com.codegym.smartphonemanagement.service.product.user.IUserProductService;
import com.codegym.smartphonemanagement.service.profile.IRecentlyViewedService;
import com.codegym.smartphonemanagement.service.wishlist.IWishlistService;
import com.codegym.smartphonemanagement.service.recommendation.RecommendationService;
import com.codegym.smartphonemanagement.model.Product;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user/products")
public class UserProductController {

    private final IUserProductService userProductService;
    private final DiscountService discountService;
    private final IRecentlyViewedService recentlyViewedService;
    private final IWishlistService wishlistService;
    private final RecommendationService recommendationService;

    private static final Long USER_ID = 1L; // Thay bằng Security context sau

    @GetMapping
    public String listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> brands,
            @RequestParam(required = false) List<String> rams,
            @RequestParam(required = false) List<String> storages,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minScreen,
            @RequestParam(required = false) Double maxScreen,
            @RequestParam(required = false) Integer minBattery,
            @RequestParam(required = false) Integer maxBattery,
            @RequestParam(required = false) Double minWeight,
            @RequestParam(required = false) Double maxWeight,
            @RequestParam(required = false) List<String> osList,
            @RequestParam(required = false) Boolean inStockOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "asc") String sort,
            Model model
    ) {

        Page<ProductResponseDTO> productPage = userProductService.searchProducts(
                keyword, brands, rams, storages, minPrice, maxPrice,
                minScreen, maxScreen, minBattery, maxBattery, minWeight, maxWeight, osList, inStockOnly,
                page, size, sort
        );

        model.addAttribute("availableBrands", userProductService.getAvailableBrands());
        model.addAttribute("availableRams", userProductService.getAvailableRams());
        model.addAttribute("availableStorages", userProductService.getAvailableStorages());

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalElements", productPage.getTotalElements()); // Add total count for results badge

        // Giữ lại filter để không mất khi phân trang
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedBrands", brands);
        model.addAttribute("selectedRams", rams);
        model.addAttribute("selectedStorages", storages);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("minScreen", minScreen);
        model.addAttribute("maxScreen", maxScreen);
        model.addAttribute("minBattery", minBattery);
        model.addAttribute("maxBattery", maxBattery);
        model.addAttribute("minWeight", minWeight);
        model.addAttribute("maxWeight", maxWeight);
        model.addAttribute("selectedOs", osList);
        model.addAttribute("inStockOnly", inStockOnly);
        model.addAttribute("sort", sort);

        // Add wishlist product IDs for heart icon highlighting
        try {
            List<Long> wishlistProductIds = wishlistService.getWishlistProductsByUserId(USER_ID)
                    .stream()
                    .map(p -> p.getId())
                    .collect(Collectors.toList());
            model.addAttribute("wishlistProductIds", wishlistProductIds);
        } catch (Exception e) {
            model.addAttribute("wishlistProductIds", List.of());
        }

        // Add breadcrumb navigation
        List<BreadcrumbItem> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(BreadcrumbItem.builder()
                .label("Trang chủ")
                .url("/user/products")
                .active(false)
                .build());
        breadcrumbs.add(BreadcrumbItem.builder()
                .label("Sản phẩm")
                .url(null)
                .active(true)
                .build());
        model.addAttribute("breadcrumbs", breadcrumbs);

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

        // === Recently Viewed: Track + Display ===
        try {
            recentlyViewedService.trackView(USER_ID, id);
        } catch (Exception ignored) { /* fail-safe: không ảnh hưởng trang */ }

        // Lấy sản phẩm đã xem gần đây (trừ sản phẩm hiện tại)
        List<ProductResponseDTO> recentProducts = recentlyViewedService.getRecentProducts(USER_ID, 10)
                .stream().filter(p -> !p.getId().equals(id)).toList();
        model.addAttribute("recentProducts", recentProducts);

        // === Recommendations: Load server-side ===
        try {
            List<Product> recommendations = recommendationService.getRecommendations(id);
            boolean hasRecommendations = !recommendations.isEmpty();
            
            model.addAttribute("recommendations", recommendations);
            model.addAttribute("hasRecommendations", hasRecommendations);
            model.addAttribute("recommendationsCount", recommendations.size());
        } catch (Exception e) {
            // Fail-safe: không ảnh hưởng trang nếu recommendations lỗi
            model.addAttribute("recommendations", new ArrayList<>());
            model.addAttribute("hasRecommendations", false);
            model.addAttribute("recommendationsCount", 0);
        }

        // Add breadcrumb navigation
        List<BreadcrumbItem> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(BreadcrumbItem.builder()
                .label("Trang chủ")
                .url("/user/products")
                .active(false)
                .build());
        breadcrumbs.add(BreadcrumbItem.builder()
                .label("Sản phẩm")
                .url("/user/products")
                .active(false)
                .build());
        breadcrumbs.add(BreadcrumbItem.builder()
                .label(product.getName())
                .url(null)
                .active(true)
                .build());
        model.addAttribute("breadcrumbs", breadcrumbs);

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
                keyword, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                page, size, "asc"
        );
        return ResponseEntity.ok(productPage.getContent());
    }

    /**
     * API for search suggestions/autocomplete
     * Returns top 6 matching products based on keyword
     */
    @GetMapping("/api/search-suggestions")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSearchSuggestions(
            @RequestParam(required = false) String keyword
    ) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        // Search products with limit 6
        Page<ProductResponseDTO> productPage = userProductService.searchProducts(
                keyword, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                0, 6, "asc"
        );

        // Map to simplified response
        List<Map<String, Object>> suggestions = productPage.getContent().stream()
                .map(product -> {
                    Map<String, Object> suggestion = new HashMap<>();
                    suggestion.put("id", product.getId());
                    suggestion.put("name", product.getName());
                    suggestion.put("brand", product.getBrand());
                    suggestion.put("price", product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice());
                    suggestion.put("imageUrl", product.getImageUrl());
                    suggestion.put("stock", product.getStock());
                    
                    // Determine icon based on brand
                    String icon = "bi-phone";
                    if (product.getBrand() != null) {
                        switch (product.getBrand().toLowerCase()) {
                            case "apple" -> icon = "bi-apple";
                            case "samsung" -> icon = "bi-phone";
                            case "xiaomi" -> icon = "bi-lightning-charge";
                            case "oppo" -> icon = "bi-camera";
                            default -> icon = "bi-phone";
                        }
                    }
                    suggestion.put("icon", icon);
                    
                    return suggestion;
                })
                .toList();

        return ResponseEntity.ok(suggestions);
    }

    /**
     * API for Quick View modal
     * Returns full product details for a specific product
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<ProductResponseDTO> getProductForQuickView(@PathVariable Long id) {
        ProductResponseDTO product = userProductService.getProductById(id);
        return ResponseEntity.ok(product);
    }
}