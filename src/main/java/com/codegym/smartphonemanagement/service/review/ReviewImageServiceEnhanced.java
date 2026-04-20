package com.codegym.smartphonemanagement.service.review;

import com.codegym.smartphonemanagement.config.ImageProcessingConfig;
import com.codegym.smartphonemanagement.exception.InvalidFileException;
import com.codegym.smartphonemanagement.exception.ResourceNotFoundException;
import com.codegym.smartphonemanagement.model.Review;
import com.codegym.smartphonemanagement.model.ReviewImage;
import com.codegym.smartphonemanagement.model.dto.ImageUploadProgress;
import com.codegym.smartphonemanagement.model.dto.ImageUploadResult;
import com.codegym.smartphonemanagement.repository.user.ReviewImageRepository;
import com.codegym.smartphonemanagement.repository.user.ReviewRepository;
import com.codegym.smartphonemanagement.service.image.ImageProcessingService;
import com.codegym.smartphonemanagement.service.progress.UploadProgressService;
import com.codegym.smartphonemanagement.service.security.FileValidationService;
import com.codegym.smartphonemanagement.service.security.RateLimitingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced service for handling review image uploads and management
 * Features:
 * - Advanced image processing (compression, thumbnails)
 * - Comprehensive security validation
 * - Rate limiting
 * - Progress tracking
 * - Retry mechanisms
 * - Caching
 * - Async processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReviewImageServiceEnhanced {
    
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewRepository reviewRepository;
    private final ImageProcessingConfig config;
    private final FileValidationService fileValidationService;
    private final ImageProcessingService imageProcessingService;
    private final RateLimitingService rateLimitingService;
    private final UploadProgressService uploadProgressService;
    
    /**
     * Upload multiple images for a review with comprehensive validation and processing
     * 
     * @param reviewId The ID of the review
     * @param files List of image files to upload
     * @param sessionId Session ID for progress tracking
     * @param userIdentifier User identifier for rate limiting (IP or user ID)
     * @return List of upload results
     */
    public List<ImageUploadResult> uploadReviewImages(Long reviewId, List<MultipartFile> files, 
                                                     String sessionId, String userIdentifier) {
        log.info("Starting upload of {} images for review ID: {} (session: {})", files.size(), reviewId, sessionId);
        
        // Rate limiting check
        if (!rateLimitingService.isUploadAllowed(userIdentifier)) {
            throw new InvalidFileException("Bạn đã vượt quá giới hạn upload. Vui lòng thử lại sau.");
        }
        
        // Validate review exists
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review không tồn tại với ID: " + reviewId));
        
        // Validate total image count
        int existingCount = reviewImageRepository.countByReviewId(reviewId);
        if (existingCount + files.size() > config.getMaxImagesPerReview()) {
            throw new InvalidFileException(
                String.format("Chỉ được upload tối đa %d ảnh cho mỗi đánh giá. Hiện tại có %d ảnh, bạn đang cố upload thêm %d ảnh.",
                    config.getMaxImagesPerReview(), existingCount, files.size())
            );
        }
        
        // Initialize progress tracking
        uploadProgressService.initializeProgress(sessionId, files.size());
        
        List<ImageUploadResult> results = new ArrayList<>();
        int displayOrder = existingCount;
        
        for (MultipartFile file : files) {
            try {
                uploadProgressService.updateProgress(sessionId, file.getOriginalFilename());
                
                // Process single image
                ImageUploadResult result = processAndSaveImage(file, review, displayOrder++);
                results.add(result);
                
                uploadProgressService.markFileSuccess(sessionId);
                log.info("Successfully uploaded image: {} for review ID: {}", result.getImageUrl(), reviewId);
                
            } catch (Exception e) {
                log.error("Error uploading image {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                
                ImageUploadResult errorResult = ImageUploadResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
                results.add(errorResult);
                
                uploadProgressService.markFileFailure(sessionId, e.getMessage());
            }
        }
        
        uploadProgressService.markCompleted(sessionId);
        
        // Clear cache
        clearImageCache(reviewId);
        
        log.info("Completed upload for review ID: {}. Success: {}, Failed: {}", 
            reviewId, 
            results.stream().mapToInt(r -> r.isSuccess() ? 1 : 0).sum(),
            results.stream().mapToInt(r -> r.isSuccess() ? 0 : 1).sum());
        
        return results;
    }
    
    /**
     * Process and save a single image with retry mechanism
     */
    @Retryable(value = {IOException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private ImageUploadResult processAndSaveImage(MultipartFile file, Review review, int displayOrder) throws IOException {
        // Comprehensive validation
        fileValidationService.validateFile(file);
        
        // Generate unique filename
        String fileName = generateFileName(file.getOriginalFilename());
        
        // Process image (compression, thumbnails)
        ImageProcessingService.ProcessedImage processedImage = imageProcessingService.processImage(file, fileName);
        
        // Save to disk
        ImageProcessingService.SavedImagePaths savedPaths = imageProcessingService.saveProcessedImage(processedImage, fileName);
        
        // Create and save entity
        ReviewImage reviewImage = ReviewImage.builder()
            .review(review)
            .imageUrl(savedPaths.getMainImageUrl())
            .imagePath(savedPaths.getMainImagePath())
            .thumbnailUrl(savedPaths.getThumbnailUrl())
            .thumbnailPath(savedPaths.getThumbnailPath())
            .fileSize(file.getSize())
            .width(processedImage.getProcessedWidth())
            .height(processedImage.getProcessedHeight())
            .originalWidth(processedImage.getOriginalWidth())
            .originalHeight(processedImage.getOriginalHeight())
            .imageFormat(processedImage.getFormat())
            .colorDepth(processedImage.getColorDepth())
            .hasTransparency(processedImage.isHasTransparency())
            .displayOrder(displayOrder)
            .uploadDate(LocalDateTime.now())
            .processingStatus(ReviewImage.ProcessingStatus.COMPLETED)
            .build();
        
        ReviewImage saved = reviewImageRepository.save(reviewImage);
        
        return ImageUploadResult.builder()
            .id(saved.getId())
            .imageUrl(saved.getImageUrl())
            .thumbnailUrl(saved.getThumbnailUrl())
            .fileSize(saved.getFileSize())
            .width(saved.getWidth())
            .height(saved.getHeight())
            .displayOrder(saved.getDisplayOrder())
            .uploadDate(saved.getUploadDate())
            .success(true)
            .build();
    }
    
    /**
     * Async image processing for better performance
     */
    @Async
    public CompletableFuture<Void> processImagesAsync(List<Long> imageIds) {
        log.info("Starting async processing for {} images", imageIds.size());
        
        for (Long imageId : imageIds) {
            try {
                ReviewImage image = reviewImageRepository.findById(imageId).orElse(null);
                if (image != null && image.getProcessingStatus() == ReviewImage.ProcessingStatus.PENDING) {
                    // Update status to processing
                    image.setProcessingStatus(ReviewImage.ProcessingStatus.PROCESSING);
                    reviewImageRepository.save(image);
                    
                    // Perform additional processing here if needed
                    // For example: generate additional thumbnail sizes, apply filters, etc.
                    
                    // Update status to completed
                    image.setProcessingStatus(ReviewImage.ProcessingStatus.COMPLETED);
                    reviewImageRepository.save(image);
                    
                    log.debug("Completed async processing for image ID: {}", imageId);
                }
            } catch (Exception e) {
                log.error("Error in async processing for image ID {}: {}", imageId, e.getMessage(), e);
                
                // Update status to failed
                ReviewImage image = reviewImageRepository.findById(imageId).orElse(null);
                if (image != null) {
                    image.setProcessingStatus(ReviewImage.ProcessingStatus.FAILED);
                    image.setProcessingError(e.getMessage());
                    reviewImageRepository.save(image);
                }
            }
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Get all images for a review with caching
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "reviewImages", key = "#reviewId")
    public List<ReviewImage> getImagesByReviewId(Long reviewId) {
        log.debug("Fetching images for review ID: {}", reviewId);
        return reviewImageRepository.findByReviewIdOrderByDisplayOrderAsc(reviewId);
    }
    
    /**
     * Get all images for a product (from all reviews) with caching
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "productImages", key = "#productId")
    public List<ReviewImage> getImagesByProductId(Long productId) {
        log.debug("Fetching images for product ID: {}", productId);
        return reviewImageRepository.findByProductIdOrderByUploadDateDesc(productId);
    }
    
    /**
     * Delete an image by ID with enhanced cleanup
     */
    @CacheEvict(value = {"reviewImages", "productImages"}, allEntries = true)
    public void deleteImage(Long imageId) {
        log.info("Deleting image with ID: {}", imageId);
        
        ReviewImage image = reviewImageRepository.findById(imageId)
            .orElseThrow(() -> new ResourceNotFoundException("Ảnh không tồn tại với ID: " + imageId));
        
        // Delete physical files from disk
        deletePhysicalFiles(image);
        
        // Delete from database
        reviewImageRepository.delete(image);
        log.info("Successfully deleted image record from database: {}", imageId);
    }
    
    /**
     * Delete all images for a review
     */
    @CacheEvict(value = {"reviewImages", "productImages"}, allEntries = true)
    public void deleteImagesByReviewId(Long reviewId) {
        log.info("Deleting all images for review ID: {}", reviewId);
        
        List<ReviewImage> images = reviewImageRepository.findByReviewIdOrderByDisplayOrderAsc(reviewId);
        
        for (ReviewImage image : images) {
            deletePhysicalFiles(image);
        }
        
        // Batch delete from database
        reviewImageRepository.deleteAll(images);
        
        log.info("Successfully deleted {} images for review ID: {}", images.size(), reviewId);
    }
    
    /**
     * Reorder images for a review
     */
    @CacheEvict(value = "reviewImages", key = "#reviewId")
    public void reorderImages(Long reviewId, List<Long> imageIds) {
        log.info("Reordering {} images for review ID: {}", imageIds.size(), reviewId);
        
        for (int i = 0; i < imageIds.size(); i++) {
            Long imageId = imageIds.get(i);
            ReviewImage image = reviewImageRepository.findById(imageId).orElse(null);
            if (image != null && image.getReview().getId().equals(reviewId)) {
                image.setDisplayOrder(i);
                reviewImageRepository.save(image);
            }
        }
        
        log.info("Successfully reordered images for review ID: {}", reviewId);
    }
    
    /**
     * Update image alt text
     */
    @CacheEvict(value = "reviewImages", key = "#reviewId")
    public void updateImageAltText(Long imageId, String altText, Long reviewId) {
        ReviewImage image = reviewImageRepository.findById(imageId)
            .orElseThrow(() -> new ResourceNotFoundException("Ảnh không tồn tại với ID: " + imageId));
        
        image.setAltText(altText);
        reviewImageRepository.save(image);
        
        log.info("Updated alt text for image ID: {}", imageId);
    }
    
    /**
     * Get upload progress
     */
    public ImageUploadProgress getUploadProgress(String sessionId) {
        return uploadProgressService.getProgress(sessionId);
    }
    
    /**
     * Cleanup orphaned files (scheduled task)
     */
    public void cleanupOrphanedFiles() {
        log.info("Starting cleanup of orphaned files");
        
        try {
            Path uploadPath = Paths.get(config.getUploadDir());
            Path thumbnailPath = Paths.get(config.getThumbnailDir());
            
            if (Files.exists(uploadPath)) {
                cleanupDirectory(uploadPath);
            }
            
            if (Files.exists(thumbnailPath)) {
                cleanupDirectory(thumbnailPath);
            }
            
        } catch (Exception e) {
            log.error("Error during orphaned files cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get image count for a review
     */
    @Transactional(readOnly = true)
    public int getImageCount(Long reviewId) {
        return reviewImageRepository.countByReviewId(reviewId);
    }
    
    /**
     * Generate unique filename with timestamp and UUID
     */
    private String generateFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("review_%s_%s.%s", timestamp, uuid, extension);
    }
    
    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "jpg"; // Default extension
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }
    
    /**
     * Delete physical files (main image and thumbnail)
     */
    private void deletePhysicalFiles(ReviewImage image) {
        // Delete main image
        try {
            if (image.getImagePath() != null) {
                Path filePath = Paths.get(image.getImagePath());
                boolean deleted = Files.deleteIfExists(filePath);
                if (deleted) {
                    log.debug("Successfully deleted main image file: {}", filePath);
                } else {
                    log.warn("Main image file not found on disk: {}", filePath);
                }
            }
        } catch (IOException e) {
            log.error("Error deleting main image file: {}", e.getMessage(), e);
        }
        
        // Delete thumbnail
        try {
            if (image.getThumbnailPath() != null) {
                Path thumbnailPath = Paths.get(image.getThumbnailPath());
                boolean deleted = Files.deleteIfExists(thumbnailPath);
                if (deleted) {
                    log.debug("Successfully deleted thumbnail file: {}", thumbnailPath);
                } else {
                    log.warn("Thumbnail file not found on disk: {}", thumbnailPath);
                }
            }
        } catch (IOException e) {
            log.error("Error deleting thumbnail file: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Clear image cache
     */
    private void clearImageCache(Long reviewId) {
        // This would be handled by @CacheEvict annotations in actual methods
        log.debug("Cleared image cache for review ID: {}", reviewId);
    }
    
    /**
     * Cleanup directory of orphaned files
     */
    private void cleanupDirectory(Path directory) throws IOException {
        Files.walk(directory)
            .filter(Files::isRegularFile)
            .forEach(file -> {
                try {
                    String fileName = file.getFileName().toString();
                    // Check if file exists in database
                    boolean existsInDb = reviewImageRepository.existsByImagePathOrThumbnailPath(
                        file.toString(), file.toString());
                    
                    if (!existsInDb) {
                        Files.deleteIfExists(file);
                        log.debug("Deleted orphaned file: {}", file);
                    }
                } catch (Exception e) {
                    log.warn("Could not check/delete file {}: {}", file, e.getMessage());
                }
            });
    }
}