package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.exception.InvalidFileException;
import com.codegym.smartphonemanagement.exception.ResourceNotFoundException;
import com.codegym.smartphonemanagement.model.ReviewImage;
import com.codegym.smartphonemanagement.service.review.ReviewImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller xử lý upload và quản lý ảnh đánh giá sản phẩm
 * Sử dụng @Controller với @ResponseBody cho các AJAX endpoints
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/user/reviews")
@Slf4j
public class ReviewImageController {
    
    private final ReviewImageService reviewImageService;
    
    /**
     * Upload ảnh cho đánh giá
     * Endpoint: POST /user/reviews/{reviewId}/images
     * 
     * @param reviewId ID của review
     * @param files Danh sách file ảnh (tối đa 5 ảnh)
     * @return JSON response với thông tin ảnh đã upload
     */
    @PostMapping("/{reviewId}/images")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadImages(
            @PathVariable Long reviewId,
            @RequestParam("images") List<MultipartFile> files) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Uploading {} images for review ID: {}", files.size(), reviewId);
            
            // Validate có file không
            if (files == null || files.isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất một ảnh");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Upload images
            List<ReviewImage> uploadedImages = reviewImageService.uploadReviewImages(reviewId, files);
            
            // Build success response
            response.put("success", true);
            response.put("message", String.format("Đã upload thành công %d ảnh", uploadedImages.size()));
            response.put("images", uploadedImages);
            response.put("totalImages", reviewImageService.getImageCount(reviewId));
            
            log.info("Successfully uploaded {} images for review ID: {}", uploadedImages.size(), reviewId);
            return ResponseEntity.ok(response);
            
        } catch (InvalidFileException e) {
            log.warn("File validation failed for review ID {}: {}", reviewId, e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (ResourceNotFoundException e) {
            log.warn("Review not found: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
            
        } catch (Exception e) {
            log.error("Unexpected error uploading images for review ID {}: {}", reviewId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Đã xảy ra lỗi khi upload ảnh. Vui lòng thử lại.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Lấy danh sách ảnh của một review
     * Endpoint: GET /user/reviews/{reviewId}/images
     * 
     * @param reviewId ID của review
     * @return JSON response với danh sách ảnh
     */
    @GetMapping("/{reviewId}/images")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getImages(@PathVariable Long reviewId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.debug("Fetching images for review ID: {}", reviewId);
            
            List<ReviewImage> images = reviewImageService.getImagesByReviewId(reviewId);
            
            response.put("success", true);
            response.put("images", images);
            response.put("totalImages", images.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching images for review ID {}: {}", reviewId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Không thể tải danh sách ảnh");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Xóa một ảnh
     * Endpoint: DELETE /user/reviews/images/{imageId}
     * 
     * @param imageId ID của ảnh cần xóa
     * @return JSON response xác nhận xóa thành công
     */
    @DeleteMapping("/images/{imageId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteImage(@PathVariable Long imageId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Deleting image ID: {}", imageId);
            
            reviewImageService.deleteImage(imageId);
            
            response.put("success", true);
            response.put("message", "Đã xóa ảnh thành công");
            
            log.info("Successfully deleted image ID: {}", imageId);
            return ResponseEntity.ok(response);
            
        } catch (ResourceNotFoundException e) {
            log.warn("Image not found: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
            
        } catch (Exception e) {
            log.error("Error deleting image ID {}: {}", imageId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Không thể xóa ảnh. Vui lòng thử lại.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Xóa tất cả ảnh của một review
     * Endpoint: DELETE /user/reviews/{reviewId}/images
     * 
     * @param reviewId ID của review
     * @return JSON response xác nhận xóa thành công
     */
    @DeleteMapping("/{reviewId}/images")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteAllImages(@PathVariable Long reviewId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Deleting all images for review ID: {}", reviewId);
            
            reviewImageService.deleteImagesByReviewId(reviewId);
            
            response.put("success", true);
            response.put("message", "Đã xóa tất cả ảnh thành công");
            
            log.info("Successfully deleted all images for review ID: {}", reviewId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error deleting images for review ID {}: {}", reviewId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Không thể xóa ảnh. Vui lòng thử lại.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Lấy số lượng ảnh của một review
     * Endpoint: GET /user/reviews/{reviewId}/images/count
     * 
     * @param reviewId ID của review
     * @return JSON response với số lượng ảnh
     */
    @GetMapping("/{reviewId}/images/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getImageCount(@PathVariable Long reviewId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int count = reviewImageService.getImageCount(reviewId);
            
            response.put("success", true);
            response.put("count", count);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting image count for review ID {}: {}", reviewId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Không thể lấy số lượng ảnh");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
