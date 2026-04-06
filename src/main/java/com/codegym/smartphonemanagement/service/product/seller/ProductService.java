package com.codegym.smartphonemanagement.service.product.seller;

import com.codegym.smartphonemanagement.exception.product.BadRequestException;
import com.codegym.smartphonemanagement.exception.product.ResourceNotFoundException;
import com.codegym.smartphonemanagement.model.Category;
import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.repository.CategoryRepository;
import com.codegym.smartphonemanagement.repository.ProductRepository;
import com.codegym.smartphonemanagement.service.product.DTO.ProductRequestDTO;
import com.codegym.smartphonemanagement.service.product.DTO.ProductResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
                .stock(request.getStock())
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
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .categoryName(product.getCategory().getName())
                .active(product.getActive())
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
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
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
}