package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ProductRepositoryUser extends JpaRepository<Product,Long> {
    @Query("""
    SELECT p FROM Product p
    WHERE p.active = true
    AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
    AND (:brand IS NULL OR p.brand = :brand)
    AND (:minPrice IS NULL OR p.price >= :minPrice)
    AND (:maxPrice IS NULL OR p.price <= :maxPrice)
""")
    Page<Product> searchForUser(
            @Param("keyword") String keyword,
            @Param("brand") String brand,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
}
