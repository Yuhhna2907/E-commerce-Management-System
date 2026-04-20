package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.ProductRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository cho ProductRecommendation entity
 * Xử lý các truy vấn liên quan đến gợi ý sản phẩm
 * 
 * Yêu cầu: 5.2, 6.1
 */
@Repository
public interface ProductRecommendationRepository extends JpaRepository<ProductRecommendation, Long> {
    
    /**
     * Tìm tất cả gợi ý cho một sản phẩm với recommendedProduct được eager load
     * Chỉ lấy những gợi ý hợp lệ (frequency >= minFrequency và count >= minCount)
     * 
     * @param productId ID của sản phẩm gốc
     * @param minFrequency Tần suất tối thiểu (mặc định 0.05 = 5%)
     * @param minCount Số lần mua chung tối thiểu (mặc định 10)
     * @return Danh sách gợi ý sắp xếp theo tần suất giảm dần
     */
    @Query("SELECT pr FROM ProductRecommendation pr " +
           "JOIN FETCH pr.recommendedProduct rp " +
           "WHERE pr.product.id = :productId " +
           "AND pr.coPurchaseFrequency >= :minFrequency " +
           "AND pr.coPurchaseCount >= :minCount " +
           "AND rp.active = true " +
           "ORDER BY pr.coPurchaseFrequency DESC")
    List<ProductRecommendation> findValidRecommendationsByProductId(
            @Param("productId") Long productId,
            @Param("minFrequency") BigDecimal minFrequency,
            @Param("minCount") Integer minCount);
    
    /**
     * Tìm top N gợi ý cho một sản phẩm
     * 
     * @param productId ID của sản phẩm gốc
     * @param minFrequency Tần suất tối thiểu
     * @param minCount Số lần mua chung tối thiểu
     * @param limit Số lượng gợi ý tối đa
     * @return Danh sách top N gợi ý
     */
    @Query(value = "SELECT pr.* FROM product_recommendations pr " +
                   "JOIN products p ON pr.recommended_product_id = p.id " +
                   "WHERE pr.product_id = :productId " +
                   "AND pr.co_purchase_frequency >= :minFrequency " +
                   "AND pr.co_purchase_count >= :minCount " +
                   "AND p.active = true " +
                   "ORDER BY pr.co_purchase_frequency DESC " +
                   "LIMIT :limit", 
           nativeQuery = true)
    List<ProductRecommendation> findTopRecommendationsByProductId(
            @Param("productId") Long productId,
            @Param("minFrequency") BigDecimal minFrequency,
            @Param("minCount") Integer minCount,
            @Param("limit") Integer limit);
    
    /**
     * Tìm gợi ý cụ thể cho một cặp sản phẩm
     * 
     * @param productId ID sản phẩm gốc
     * @param recommendedProductId ID sản phẩm được gợi ý
     * @return Optional chứa ProductRecommendation nếu tồn tại
     */
    Optional<ProductRecommendation> findByProductIdAndRecommendedProductId(
            Long productId, Long recommendedProductId);
    
    /**
     * Kiểm tra xem có gợi ý nào cho sản phẩm không
     * 
     * @param productId ID của sản phẩm
     * @return true nếu có ít nhất 1 gợi ý hợp lệ
     */
    @Query("SELECT COUNT(pr) > 0 FROM ProductRecommendation pr " +
           "WHERE pr.product.id = :productId " +
           "AND pr.coPurchaseFrequency >= 0.01 " +
           "AND pr.coPurchaseCount >= 2")
    boolean hasValidRecommendations(@Param("productId") Long productId);
    
    /**
     * Đếm số lượng gợi ý hợp lệ cho một sản phẩm
     * 
     * @param productId ID của sản phẩm
     * @return Số lượng gợi ý hợp lệ
     */
    @Query("SELECT COUNT(pr) FROM ProductRecommendation pr " +
           "WHERE pr.product.id = :productId " +
           "AND pr.coPurchaseFrequency >= 0.01 " +
           "AND pr.coPurchaseCount >= 2")
    long countValidRecommendations(@Param("productId") Long productId);
    
    /**
     * Xóa tất cả gợi ý cho một sản phẩm (dùng khi rebuild)
     * 
     * @param productId ID của sản phẩm
     */
    @Modifying
    @Query("DELETE FROM ProductRecommendation pr WHERE pr.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
    
    /**
     * Xóa tất cả gợi ý cũ (dùng khi rebuild toàn bộ)
     */
    @Modifying
    @Query("DELETE FROM ProductRecommendation")
    void deleteAll();
    
    /**
     * Tìm tất cả sản phẩm có gợi ý (để rebuild)
     * 
     * @return Danh sách ID các sản phẩm có gợi ý
     */
    @Query("SELECT DISTINCT pr.product.id FROM ProductRecommendation pr")
    List<Long> findAllProductIdsWithRecommendations();
    
    /**
     * Cập nhật thông tin co-purchase cho một cặp sản phẩm
     * 
     * @param productId ID sản phẩm gốc
     * @param recommendedProductId ID sản phẩm được gợi ý
     * @param count Số lần mua chung
     * @param frequency Tần suất mua chung
     */
    @Modifying
    @Query("UPDATE ProductRecommendation pr " +
           "SET pr.coPurchaseCount = :count, " +
           "    pr.coPurchaseFrequency = :frequency, " +
           "    pr.lastUpdated = CURRENT_TIMESTAMP " +
           "WHERE pr.product.id = :productId " +
           "AND pr.recommendedProduct.id = :recommendedProductId")
    int updateCoPurchaseData(
            @Param("productId") Long productId,
            @Param("recommendedProductId") Long recommendedProductId,
            @Param("count") Integer count,
            @Param("frequency") BigDecimal frequency);
    
    /**
     * Tìm các gợi ý cần cập nhật (cũ hơn 1 ngày)
     * Sử dụng native query vì HQL không hỗ trợ INTERVAL
     * 
     * @return Danh sách ProductRecommendation cần cập nhật
     */
    @Query(value = "SELECT * FROM product_recommendations " +
                   "WHERE last_updated < DATE_SUB(NOW(), INTERVAL 1 DAY)", 
           nativeQuery = true)
    List<ProductRecommendation> findOutdatedRecommendations();
    
    /**
     * Lấy thống kê tổng quan về recommendations
     * 
     * @return Array chứa [totalRecommendations, validRecommendations, uniqueProducts]
     */
    @Query("SELECT " +
           "COUNT(pr), " +
           "SUM(CASE WHEN pr.coPurchaseFrequency >= 0.01 AND pr.coPurchaseCount >= 2 THEN 1 ELSE 0 END), " +
           "COUNT(DISTINCT pr.product.id) " +
           "FROM ProductRecommendation pr")
    Object[] getRecommendationStats();
}