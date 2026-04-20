package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {
    
    /**
     * Tìm tất cả ảnh của một review, sắp xếp theo display order
     */
    List<ReviewImage> findByReviewIdOrderByDisplayOrderAsc(Long reviewId);
    
    /**
     * Đếm số lượng ảnh của một review
     */
    @Query("SELECT COUNT(ri) FROM ReviewImage ri WHERE ri.review.id = :reviewId")
    int countByReviewId(@Param("reviewId") Long reviewId);
    
    /**
     * Tìm tất cả ảnh của các review thuộc một sản phẩm, sắp xếp theo ngày upload mới nhất
     */
    @Query("SELECT ri FROM ReviewImage ri WHERE ri.review.product.id = :productId ORDER BY ri.uploadDate DESC")
    List<ReviewImage> findByProductIdOrderByUploadDateDesc(@Param("productId") Long productId);
    
    /**
     * Xóa tất cả ảnh của một review
     */
    void deleteByReviewId(Long reviewId);
    
    /**
     * Check if file exists in database by image path or thumbnail path
     */
    @Query("SELECT COUNT(ri) > 0 FROM ReviewImage ri WHERE ri.imagePath = :imagePath OR ri.thumbnailPath = :thumbnailPath")
    boolean existsByImagePathOrThumbnailPath(@Param("imagePath") String imagePath, @Param("thumbnailPath") String thumbnailPath);
    
    /**
     * Find images by processing status
     */
    List<ReviewImage> findByProcessingStatus(ReviewImage.ProcessingStatus status);
    
    /**
     * Find images that need cleanup (orphaned files)
     */
    @Query("SELECT ri FROM ReviewImage ri WHERE ri.processingStatus = 'FAILED' AND ri.uploadDate < :cutoffDate")
    List<ReviewImage> findFailedImagesOlderThan(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}
