package com.codegym.smartphonemanagement.service.product.user;

import com.codegym.smartphonemanagement.service.product.DTO.ProductResponseDTO;
import com.codegym.smartphonemanagement.service.product.DTO.ReviewRequestDTO;
import com.codegym.smartphonemanagement.service.product.DTO.ReviewResponseDTO;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface IUserProductService {
    Page<ProductResponseDTO> searchProducts(
            String keyword,
            List<String> brands,
            List<String> rams,
            List<String> storages,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minScreen,
            Double maxScreen,
            Integer minBattery,
            Integer maxBattery,
            Double minWeight,
            Double maxWeight,
            List<String> osList,
            Boolean inStockOnly,
            int page,
            int size,
            String sortDirection
    );

    ProductResponseDTO getProductById(Long id);

    ReviewResponseDTO reviewProduct(Long userId, ReviewRequestDTO request);

    List<ReviewResponseDTO> getReviewsByProductId(Long id);

    List<com.codegym.smartphonemanagement.service.product.DTO.ComparisonItemDTO> compareProducts(List<Long> productIds);

    List<String> getAvailableBrands();
    List<String> getAvailableRams();
    List<String> getAvailableStorages();
}
