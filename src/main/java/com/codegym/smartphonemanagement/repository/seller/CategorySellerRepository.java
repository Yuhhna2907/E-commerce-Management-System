package com.codegym.smartphonemanagement.repository.seller;

import com.codegym.smartphonemanagement.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategorySellerRepository extends JpaRepository<Category, Long> {

    // Tìm các danh mục đang hoạt động
    List<Category> findAllByActiveTrue();

    // Kiểm tra tên trùng (dùng cho thêm/sửa)
    boolean existsByNameAndIdNot(String name, Long id);
    boolean existsByName(String name);
    
    // Case-insensitive uniqueness checks
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    
    // Search methods with pagination
    Page<Category> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
    Page<Category> findAllByActive(Boolean active, Pageable pageable);
    Page<Category> findByNameContainingIgnoreCaseAndActive(String keyword, Boolean active, Pageable pageable);
    
    // ===== BRAND MANAGEMENT QUERIES =====
    
    /**
     * Get brand statistics (brand name and product count)
     */
    @Query("SELECT p.brand, COUNT(p) FROM Product p WHERE p.brand IS NOT NULL AND p.brand != '' GROUP BY p.brand ORDER BY COUNT(p) DESC")
    List<Object[]> getBrandStatistics();
    
    /**
     * Get all distinct brand names
     */
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL AND p.brand != '' ORDER BY p.brand")
    List<String> findDistinctBrands();
    
    /**
     * Get total products count
     */
    @Query("SELECT COUNT(p) FROM Product p")
    Long getTotalProductsCount();
    
    /**
     * Check if brand exists
     */
    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.brand = :brand")
    boolean existsByBrand(@Param("brand") String brand);
    
    /**
     * Rename brand across all products
     */
    @Modifying
    @Query("UPDATE Product p SET p.brand = :newName WHERE p.brand = :oldName")
    int renameBrand(@Param("oldName") String oldName, @Param("newName") String newName);
    
    /**
     * Merge source brand into target brand
     */
    @Modifying
    @Query("UPDATE Product p SET p.brand = :targetBrand WHERE p.brand = :sourceBrand")
    int mergeBrands(@Param("sourceBrand") String sourceBrand, @Param("targetBrand") String targetBrand);
    
    /**
     * Delete brand (set to null or default value)
     */
    @Modifying
    @Query("UPDATE Product p SET p.brand = 'Unknown' WHERE p.brand = :brandName")
    int deleteBrand(@Param("brandName") String brandName);
    
    // ===== BULK OPERATIONS =====
    
    /**
     * Count products by category ID
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    Long countProductsByCategoryId(@Param("categoryId") Long categoryId);
    
    /**
     * Find all categories with product counts using native query for better performance
     */
    @Query(value = "SELECT c.id, c.name, c.active, COUNT(p.id) as product_count " +
                   "FROM categories c " +
                   "LEFT JOIN products p ON c.id = p.category_id " +
                   "GROUP BY c.id, c.name, c.active " +
                   "ORDER BY c.id", 
           nativeQuery = true)
    List<Object[]> findAllWithProductCounts();
    
    /**
     * Bulk update category status
     */
    @Modifying
    @Query("UPDATE Category c SET c.active = :status WHERE c.id IN :categoryIds")
    int bulkUpdateStatus(@Param("categoryIds") List<Long> categoryIds, @Param("status") Boolean status);
}