package com.codegym.smartphonemanagement.service.product.seller;

import com.codegym.smartphonemanagement.exception.BadRequestException;
import com.codegym.smartphonemanagement.exception.ResourceNotFoundException;
import com.codegym.smartphonemanagement.model.Category;
import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.repository.user.CartItemRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService implements IProductService {
    // 🔹 CREATE
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CartItemRepository cartItemRepository; // FIX #5: Thêm dependency

    public ProductResponseDTO create(ProductRequestDTO request) {

        // FIX #4: Kiểm tra duplicate product name
        if (productRepository.existsByNameAndActiveTrue(request.getName())) {
            throw new BadRequestException("Sản phẩm với tên '" + request.getName() + "' đã tồn tại");
        }

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

        // FIX #5: Kiểm tra có trong giỏ hàng không
        long cartCount = cartItemRepository.countByProductId(id);
        if (cartCount > 0) {
            throw new BadRequestException(
                "Không thể xóa sản phẩm này vì đang có " + cartCount + " người dùng có trong giỏ hàng. " +
                "Vui lòng đợi họ thanh toán hoặc xóa khỏi giỏ."
            );
        }

        product.setActive(false);
        product.setUpdatedAt(LocalDateTime.now());

        productRepository.save(product);
    }

    @Override
    public Page<ProductResponseDTO> search(
            String keyword,
            Long categoryId,
            java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        // 1. Kiểm tra tính hợp lệ của phân trang
        if (page < 0) page = 0;
        if (size <= 0 || size > 50) size = 10;

        // 2. Xử lý sắp xếp (Bảo vệ chống lỗi nếu sortBy null)
        String sortProperty = (sortBy == null || sortBy.isEmpty()) ? "id" : sortBy;
        Sort sort = direction.equalsIgnoreCase("asc") ?
                Sort.by(sortProperty).ascending() :
                Sort.by(sortProperty).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // 3. Chuẩn hóa dữ liệu đầu vào (Keyword)
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        // 4. Gọi Repository với đầy đủ các tham số lọc mới
        Page<Product> productPage = productRepository.searchAndFilter(
                searchKeyword,
                categoryId,
                minPrice,
                maxPrice,
                pageable
        );

        // 5. Map sang DTO để trả về cho giao diện
        return productPage.map(this::mapToResponse);
    }

    @Override
    public ProductResponseDTO getById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product không tồn tại"));

        return mapToResponse(product);
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalProducts = productRepository.countByActiveTrue();
        long lowStockCount = productRepository.countByActiveTrueAndStockLessThan(5);
        java.math.BigDecimal totalValue = productRepository.calculateTotalInventoryValue();

        stats.put("totalProducts", totalProducts);
        stats.put("lowStockCount", lowStockCount);
        stats.put("inventoryValue", totalValue != null ? totalValue : java.math.BigDecimal.ZERO);

        return stats;
    }

    @Transactional
    public boolean toggleStatus(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        boolean newStatus = !product.getActive();
        productRepository.updateActiveStatus(id, newStatus);
        return newStatus;
    }
}