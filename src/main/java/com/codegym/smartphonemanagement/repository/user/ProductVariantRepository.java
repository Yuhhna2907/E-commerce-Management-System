package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    // Lấy tất cả biến thể theo product_id
    List<ProductVariant> findByProductId(Long productId);

    // Lấy biến thể theo SKU
    ProductVariant findBySku(String sku);

    // Lấy tất cả biến thể theo màu
    List<ProductVariant> findByColor(String color);

    // Lấy tất cả biến thể theo dung lượng
    List<ProductVariant> findByStorage(String storage);

    // Lấy tất cả biến thể theo RAM
    List<ProductVariant> findByRam(String ram);

    // Lấy biến thể theo product_id + color + storage + ram
    ProductVariant findByProductIdAndColorAndStorageAndRam(Long productId, String color, String storage, String ram);
}
