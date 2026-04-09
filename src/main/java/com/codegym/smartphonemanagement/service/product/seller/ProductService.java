package com.codegym.smartphonemanagement.service.product.seller;

import com.codegym.smartphonemanagement.exception.BadRequestException;
import com.codegym.smartphonemanagement.exception.ResourceNotFoundException;
import com.codegym.smartphonemanagement.model.Category;
import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.repository.user.CategoryRepository;
import com.codegym.smartphonemanagement.repository.seller.ProductRepository;
import com.codegym.smartphonemanagement.service.product.DTO.ProductRequestDTO;
import com.codegym.smartphonemanagement.service.product.DTO.ProductResponseDTO;

import com.codegym.smartphonemanagement.service.product.DTO.ProductVariantResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService implements IProductService {
    // 🔹 CREATE
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductResponseDTO create(ProductRequestDTO request) {

        // 1️⃣ Kiểm tra category tồn tại
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category không tồn tại"));

        // 2️⃣ Validate nghiệp vụ thêm (phòng trường hợp bypass validation)
        if (request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Giá phải lớn hơn 0");
        }

        if (request.getStock() < 0) {
            throw new BadRequestException("Số lượng không hợp lệ");
        }

        // 3️⃣ Tạo entity
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .brand(request.getBrand())
                .imageUrl(request.getImageUrl())
                .category(category)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Product saved = productRepository.save(product);

        // 4️⃣ Convert sang ResponseDTO
        return mapToResponse(saved);
    }

    private ProductResponseDTO mapToResponse(Product product) {
        // Map danh sách biến thể sang DTO
        List<ProductVariantResponseDTO> variantDTOs = product.getVariants()
                .stream()
                .map(variant -> ProductVariantResponseDTO.builder()
                        .variantId(variant.getVariantId())
                        .productId(product.getId())
                        .sku(variant.getSku())
                        .variantName(variant.getVariantName())
                        .color(variant.getColor())
                        .storage(variant.getStorage())
                        .ram(variant.getRam())
                        .costPrice(variant.getCostPrice())
                        .salePrice(variant.getSalePrice())
                        .stockQuantity(variant.getStockQuantity())
                        .active(variant.getIsActive())
                        .build()
                )
                .toList();

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .active(product.getActive())
                .variants(variantDTOs) // thêm danh sách biến thể
                .build();
    }


    // 🔹 UPDATE
    @Override
    public ProductResponseDTO update(Long id, ProductRequestDTO request) {

        // 1️⃣ Kiểm tra product tồn tại
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product không tồn tại"));

        // 2️⃣ Kiểm tra category tồn tại
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category không tồn tại"));

        if (!product.getActive()) {
            throw new BadRequestException("Không thể cập nhật sản phẩm đã bị xoá");
        }

        // 3️⃣ Validate nghiệp vụ
        if (request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Giá phải lớn hơn 0");
        }

        if (request.getStock() < 0) {
            throw new BadRequestException("Số lượng không hợp lệ");
        }

        // 4️⃣ Update field
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setUpdatedAt(LocalDateTime.now());

        Product updated = productRepository.save(product);

        return mapToResponse(updated);
    }

    // 🔹 DELETE (SOFT DELETE)
    @Override
    public void deleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product không tồn tại"));

        if (!product.getActive()) {
            throw new BadRequestException("Product đã bị xoá trước đó");
        }

        product.setActive(false);
        product.setUpdatedAt(LocalDateTime.now());

        productRepository.save(product);
    }

    @Override
    public Page<ProductResponseDTO> search(
            String keyword,
            Long categoryId,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        if (page < 0) page = 0;
        if (size <= 0 || size > 50) size = 10;

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // Nếu keyword rỗng thì set null để query dễ xử lý
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        Page<Product> productPage =
                productRepository.searchAndFilter(keyword, categoryId, pageable);

        return productPage.map(this::mapToResponse);
    }

    @Override
    public ProductResponseDTO getById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product không tồn tại"));

        return mapToResponse(product);
    }
}