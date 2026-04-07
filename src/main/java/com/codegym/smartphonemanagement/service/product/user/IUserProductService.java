package com.codegym.smartphonemanagement.service.product.user;

import com.codegym.smartphonemanagement.service.product.DTO.ProductResponseDTO;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

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
}
