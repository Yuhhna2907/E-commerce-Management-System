package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.exception.InvalidFileException;
import com.codegym.smartphonemanagement.exception.ResourceNotFoundException;
import com.codegym.smartphonemanagement.model.ReviewImage;
import com.codegym.smartphonemanagement.model.dto.ImageUploadProgress;
import com.codegym.smartphonemanagement.model.dto.ImageUploadResult;
import com.codegym.smartphonemanagement.service.review.ReviewImageService;
import com.codegym.smartphonemanagement.service.review.ReviewImageServiceEnhanced;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced controller for handling review image uploads and management
 * Features:
 * - Progress tracking
 * - Rate limiting
 * - Enhanced error handling
 * - Batch operations
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/user/reviews")
@Slf4j
public class ReviewImageController {
    
    private final ReviewImageService reviewImageService; // Legacy service for backward compatibility
    private final ReviewImageServiceEnhanced enhancedService; // New enhanced service
    
    /**
     * Enhanced upload endpoint with progress tracking and rate limiting
     * Endpoint: POST /user/reviews/{reviewId}/images/enhanced
     * 
     * @param reviewId ID của review
     * @param files Danh sách file ảnh (tối đa 5 ảnh)
     * @param request HTTP request for IP extraction
     * @return JSON response với thông tin ảnh đã upload và session ID
     */
    @PostMapping("/{reviewId}/images/enhanced")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadImagesEnhanced(
            @PathVariable Long reviewId,
            @RequestParam("images") List<MultipartFile> files,
            HttpServletRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        String sessionId = UUID.randomUUID().toString();
        String userIdentifier = getClientIpAddress(request);
        
        try {
            log.info("Enhanced upload: {} images for review ID: {} (session: {})", files.size(), reviewId, sessionId);
            
            // Validate có file không
            if (files == null || files.isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất một ảnh");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Enhanced upload with progress tracking
            List<ImageUploadResult> results = enhancedService.uploadReviewImages(
                reviewId, files, sessionId, userIdentifier);
            
            // Count successful uploads
            long successCount = results.stream().filter(ImageUploadResult::isSuccess).count();
            long failCount = results.size() - successCount;
            
            // Build response
            response.put("success", successCount > 0);
            response.put("sessionId", sessionId);
            response.put("message", String.format("Đã upload thành công %d/%d ảnh", successCount, results.size()));
            response.put("results", results);
            response.put("successCount", successCount);
            response.put("failCount", failCount);
            response.put("totalImages", enhancedService.getImageCount(reviewId));
            
            log.info("Enhanced upload completed for review ID: {}. Success: {}, Failed: {}", 
                reviewId, successCount, failCount);
            
            return ResponseEntity.ok(response);
            
        } catch (InvalidFileException e) {
            log.warn("File validation failed for review ID {}: {}", reviewId, e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("sessionId", sessionId);
            return ResponseEntity.badRequest().body(response);
            
        } catch (ResourceNotFoundException e) {
            log.warn("Review not found: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("sessionId", sessionId);
            return ResponseEntity.status(404).body(response);
            
        } catch (Exception e) {
            log.error("Unexpected error in enhanced upload for review ID {}: {}", reviewId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Đã xảy ra lỗi khi upload ảnh. Vui lòng thử lại.");
            response.put("sessionId", sessionId);
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Get upload progress
     * Endpoint: GET /user/reviews/upload-progress/{sessionId}
     */
    @GetMapping("/upload-progress/{sessionId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUploadProgress(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ImageUploadProgress progress = enhancedService.getUploadProgress(sessionId);
            
            if (progress == null) {
                response.put("success", false);
                response.put("message", "Session không tồn tại hoặc đã hết hạn");
                return ResponseEntity.status(404).body(response);
            }
            
            response.put("success", true);
            response.put("progress", progress);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting upload progress for session {}: {}", sessionId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Không thể lấy thông tin tiến trình");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Reorder images for a review
     * Endpoint: PUT /user/reviews/{reviewId}/images/reorder
     */
    @PutMapping("/{reviewId}/images/reorder")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reorderImages(
            @PathVariable Long reviewId,
            @RequestBody List<Long> imageIds) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Reordering {} images for review ID: {}", imageIds.size(), reviewId);
            
            enhancedService.reorderImages(reviewId, imageIds);
            
            response.put("success", true);
            response.put("message", "Đã sắp xếp lại thứ tự ảnh thành công");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error reordering images for review ID {}: {}", reviewId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Không thể sắp xếp lại ảnh");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Update image alt text
     * Endpoint: PUT /user/reviews/{reviewId}/images/{imageId}/alt-text
     */
    @PutMapping("/{reviewId}/images/{imageId}/alt-text")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateAltText(
            @PathVariable Long reviewId,
            @PathVariable Long imageId,
            @RequestBody Map<String, String> request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String altText = request.get("altText");
            
            enhancedService.updateImageAltText(imageId, altText, reviewId);
            
            response.put("success", true);
            response.put("message", "Đã cập nhật mô tả ảnh thành công");
            
            return ResponseEntity.ok(response);
            
        } catch (ResourceNotFoundException e) {
            log.warn("Image not found: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
            
        } catch (Exception e) {
            log.error("Error updating alt text for image ID {}: {}", imageId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Không thể cập nhật mô tả ảnh");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    // Legacy endpoints for backward compatibility
    
    /**
     * Legacy upload endpoint (backward compatibility)
     * Endpoint: POST /user/reviews/{reviewId}/images
     */
    @PostMapping("/{reviewId}/images")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadImages(
            @PathVariable Long reviewId,
            @RequestParam("images") List<MultipartFile> files) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Legacy upload: {} images for review ID: {}", files.size(), reviewId);
            
            // Validate có file không
            if (files == null || files.isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất một ảnh");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Use legacy service
            List<ReviewImage> uploadedImages = reviewImageService.uploadReviewImages(reviewId, files);
            
            // Build success response
            response.put("success", true);
            response.put("message", String.format("Đã upload thành công %d ảnh", uploadedImages.size()));
            response.put("images", uploadedImages);
            response.put("totalImages", reviewImageService.getImageCount(reviewId));
            
            log.info("Legacy upload completed for review ID: {}", reviewId);
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
     */
    @GetMapping("/{reviewId}/images")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getImages(@PathVariable Long reviewId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.debug("Fetching images for review ID: {}", reviewId);
            
            List<ReviewImage> images = enhancedService.getImagesByReviewId(reviewId);
            
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
     */
    @DeleteMapping("/images/{imageId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteImage(@PathVariable Long imageId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Deleting image ID: {}", imageId);
            
            enhancedService.deleteImage(imageId);
            
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
     */
    @DeleteMapping("/{reviewId}/images")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteAllImages(@PathVariable Long reviewId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Deleting all images for review ID: {}", reviewId);
            
            enhancedService.deleteImagesByReviewId(reviewId);
            
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
     */
    @GetMapping("/{reviewId}/images/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getImageCount(@PathVariable Long reviewId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int count = enhancedService.getImageCount(reviewId);
            
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
    
    /**
     * Get client IP address for rate limiting
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
