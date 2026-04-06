package com.codegym.smartphonemanagement.service.product.seller;

import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService implements IProductService {

    private final ProductRepository productRepository;

    // 🔹 CREATE
    @Override
    public Product createProduct(Product product) {

        if (product.getStock() != null && product.getStock() < 0) {
            throw new RuntimeException("Stock không được âm");
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
    public Product updateProduct(Long id, Product newProduct) {

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