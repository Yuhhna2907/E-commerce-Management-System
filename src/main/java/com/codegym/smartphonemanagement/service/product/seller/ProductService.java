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

        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        // Nếu có active thì nên check
        if (existing.getActive() != null && !existing.getActive()) {
            throw new RuntimeException("Sản phẩm đã bị xóa");
        }

        existing.setName(newProduct.getName());
        existing.setBrand(newProduct.getBrand());
        existing.setPrice(newProduct.getPrice());
        existing.setStock(newProduct.getStock());
        existing.setStorage(newProduct.getStorage());
        existing.setColor(newProduct.getColor());
        existing.setDescription(newProduct.getDescription());

        return productRepository.save(existing);
    }

    // 🔹 DELETE (SOFT DELETE)
    @Override
    public void deleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        // Nếu đã có active thì dùng soft delete
        if (product.getActive() != null) {
            product.setActive(false);
            productRepository.save(product);
        } else {
            // fallback nếu chưa có field active
            productRepository.delete(product);
        }
    }
}