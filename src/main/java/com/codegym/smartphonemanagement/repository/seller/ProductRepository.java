package com.codegym.smartphonemanagement.repository.seller;

import com.codegym.smartphonemanagement.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByActiveTrue(Pageable pageable);

    @Query("""
    SELECT p FROM Product p
    WHERE p.active = true
    AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
    AND (:categoryId IS NULL OR p.category.id = :categoryId)
    AND (:minPrice IS NULL OR p.price >= :minPrice)
    AND (:maxPrice IS NULL OR p.price <= :maxPrice)
""")
    Page<Product> searchAndFilter(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") java.math.BigDecimal minPrice,
            @Param("maxPrice") java.math.BigDecimal maxPrice,
            Pageable pageable
    );

    // 1. Tổng số sản phẩm đang hoạt động
    long countByActiveTrue();
    long countByActiveFalse();

    // 2. Đếm sản phẩm sắp hết hàng (stock < 5 và active = true)
    long countByActiveTrueAndStockLessThan(Integer threshold);

    // 3. Tính tổng giá trị kho hàng (Sử dụng JPQL để nhân price * stock)
    @Query("SELECT SUM(p.price * p.stock) FROM Product p WHERE p.active = true")
    java.math.BigDecimal calculateTotalInventoryValue();

    // Cập nhật trạng thái nhanh theo Id
    @Modifying
    @Query("UPDATE Product p SET p.active = :status WHERE p.id = :id")
    void updateActiveStatus(@Param("id") Long id, @Param("status") Boolean status);

    // FIX #4: Check duplicate product name
    boolean existsByNameAndActiveTrue(String name);
    
    // Get top 5 active products ordered by creation date (newest first)
    @Query("SELECT p FROM Product p WHERE p.active = :active ORDER BY p.createdAt DESC LIMIT 5")
    java.util.List<Product> findTop5ByActiveOrderByCreatedAtDesc(@Param("active") Boolean active);
    // --- ADVANCED ANALYTICS ---

    // Hàng tồn quá lâu (>90 ngày chưa bán hoặc chỉ mới tạo nhưng không có doanh số)
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.createdAt < :threshold AND p.stock > 0")
    java.util.List<Product> findAgedInventory(@Param("threshold") java.time.LocalDateTime threshold);

    // Thống kê phân khúc giá
    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.price >= :min AND p.price < :max")
    long countByPriceRange(@Param("min") java.math.BigDecimal min, @Param("max") java.math.BigDecimal max);

    // Tồn kho nhiều (Slog moving)
    List<Product> findByActiveTrueAndStockGreaterThanOrderByStockDesc(Integer threshold, org.springframework.data.domain.Pageable pageable);

    // Hết hàng
    List<Product> findByActiveTrueAndStockEquals(Integer stock);

    // Sản phẩm có stock thấp hơn threshold
    List<Product> findByActiveTrueAndStockLessThan(Integer threshold);
}
