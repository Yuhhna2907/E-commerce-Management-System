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
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortDirection
    );
    ProductResponseDTO getProductById(Long id);

    ReviewResponseDTO reviewProduct(Long userId, ReviewRequestDTO request);

    List<ReviewResponseDTO> getReviewsByProductId(Long id);

}
