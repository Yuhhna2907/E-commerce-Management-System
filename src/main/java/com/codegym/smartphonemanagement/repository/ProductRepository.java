package com.codegym.smartphonemanagement.repository;

import com.codegym.smartphonemanagement.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findBySellerIdAndActiveTrue(Long sellerId, Pageable pageable);

    Optional<Product> findByIdAndSellerId(Long id, Long sellerId);

    Page<Product> findBySellerIdAndNameContainingIgnoreCaseAndActiveTrue(
            Long sellerId,
            String name,
            Pageable pageable
    );

    Page<Product> findBySellerIdAndBrandIgnoreCaseAndActiveTrue(
            Long sellerId,
            String brand,
            Pageable pageable
    );

    Page<Product> findBySellerIdAndPriceBetweenAndActiveTrue(
            Long sellerId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    );

    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true           \s
              AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:brand IS NULL OR LOWER(p.brand) = LOWER(:brand))
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
           \s""")
    Page<Product> searchAdvanced(
            @Param("sellerId") Long sellerId,
            @Param("name") String name,
            @Param("brand") String brand,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
}
