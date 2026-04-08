package com.codegym.smartphonemanagement.service.product.user;

import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.repository.user.ProductRepositoryUser;
import com.codegym.smartphonemanagement.service.logicDiscount.DiscountService;
import com.codegym.smartphonemanagement.service.product.DTO.ProductResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserProductService implements IUserProductService {

    private final ProductRepositoryUser productRepository;
    private final DiscountService discountService;

    @Override
    public Page<ProductResponseDTO> searchProducts(
            String keyword,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortDirection
    ) {

        // Normalize dữ liệu
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        if (brand != null && brand.trim().isEmpty()) {
            brand = null;
        }

        Sort sort = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.by("price").descending()
                : Sort.by("price").ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> productPage = productRepository.searchForUser(
                keyword,
                brand,
                minPrice,
                maxPrice,
                pageable
        );

        return productPage.map(this::convertToDTO);
    }

    private ProductResponseDTO convertToDTO(Product product) {

        BigDecimal discountPrice = discountService.applyDiscount(product);
        String discountLabel = discountService.getDiscountLabel(product);
        Integer stock = product.getStock() != null ? product.getStock() : 0;
        Integer sold = product.getSold() != null ? product.getSold() : 0;
        Integer totalQuantity = stock + sold;

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .color(product.getColor())
                .description(product.getDescription())
                .price(product.getPrice())
                .discountPrice(discountPrice)
                .discountLabel(discountLabel)
                .stock(stock)
                .sold(sold)
                .totalQuantity(totalQuantity)
                .imageUrl(product.getImageUrl())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .active(product.getActive())
                .build();
    }

    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        BigDecimal discountPrice = discountService.applyDiscount(product);
        String discountLabel = discountService.getDiscountLabel(product);
        Integer stock = product.getStock() != null ? product.getStock() : 0;
        Integer sold = product.getSold() != null ? product.getSold() : 0;
        Integer totalQuantity = stock + sold;

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .color(product.getColor())
                .description(product.getDescription())
                .price(product.getPrice())
                .discountPrice(discountPrice)
                .discountLabel(discountLabel)
                .stock(stock)
                .sold(sold)
                .totalQuantity(totalQuantity)
                .imageUrl(product.getImageUrl())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .active(product.getActive())
                .build();
    }
}
