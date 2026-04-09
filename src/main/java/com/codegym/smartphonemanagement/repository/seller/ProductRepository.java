package com.codegym.smartphonemanagement.repository.seller;

import com.codegym.smartphonemanagement.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 2. Đếm sản phẩm sắp hết hàng (stock < 5 và active = true)
    long countByActiveTrueAndStockLessThan(Integer threshold);

    // 3. Tính tổng giá trị kho hàng (Sử dụng JPQL để nhân price * stock)
    @Query("SELECT SUM(p.price * p.stock) FROM Product p WHERE p.active = true")
    java.math.BigDecimal calculateTotalInventoryValue();

    // Cập nhật trạng thái nhanh theo Id
    @Modifying
    @Query("UPDATE Product p SET p.active = :status WHERE p.id = :id")
    void updateActiveStatus(@Param("id") Long id, @Param("status") Boolean status);
}
