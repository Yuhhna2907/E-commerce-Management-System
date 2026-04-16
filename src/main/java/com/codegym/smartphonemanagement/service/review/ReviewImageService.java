package com.codegym.smartphonemanagement.service.review;

import com.codegym.smartphonemanagement.exception.InvalidFileException;
import com.codegym.smartphonemanagement.exception.ResourceNotFoundException;
import com.codegym.smartphonemanagement.model.Review;
import com.codegym.smartphonemanagement.model.ReviewImage;
import com.codegym.smartphonemanagement.repository.user.ReviewImageRepository;
import com.codegym.smartphonemanagement.repository.user.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling review image uploads and management
 * Implements comprehensive validation including:
 * - File extension validation
 * - MIME type validation
 * - File signature (magic bytes) validation
 * - Image dimension validation
 * - File size validation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReviewImageService {
    
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewRepository reviewRepository;
    
    @Value("${app.upload.review-images-dir:uploads/review-images}")
    private String uploadDir;
    
    @Value("${app.upload.max-file-size:5242880}") // 5MB default
    private long maxFileSize;
    
    // Allowed file extensions
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    
    // Allowed MIME types
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/webp"
    );
    
    // Executable extensions to block (security)
    private static final List<String> BLOCKED_EXTENSIONS = Arrays.asList(
        "exe", "bat", "sh", "dll", "so", "cmd", "com", "scr", "vbs", "js", "jar", "app"
    );
    
    // Image dimension constraints
    private static final int MIN_WIDTH = 100;
    private static final int MIN_HEIGHT = 100;
    private static final int MAX_WIDTH = 4096;
    private static final int MAX_HEIGHT = 4096;
    
    // Maximum images per review
    private static final int MAX_IMAGES_PER_REVIEW = 5;
    
    // File signature (magic bytes) for validation
    private static final byte[] JPEG_SIGNATURE = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] WEBP_SIGNATURE = new byte[]{0x52, 0x49, 0x46, 0x46}; // "RIFF"
    
    /**
     * Upload multiple images for a review with comprehensive validation
     * 
     * @param reviewId The ID of the review
     * @param files List of image files to upload
     * @return List of saved ReviewImage entities
     * @throws ResourceNotFoundException if review doesn't exist
     * @throws InvalidFileException if validation fails
     */
    public List<ReviewImage> uploadReviewImages(Long reviewId, List<MultipartFile> files) {
        log.info("Starting upload of {} images for review ID: {}", files.size(), reviewId);
        
        // Validate review exists
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review không tồn tại với ID: " + reviewId));
        
        // Validate total image count
        int existingCount = reviewImageRepository.countByReviewId(reviewId);
        if (existingCount + files.size() > MAX_IMAGES_PER_REVIEW) {
            throw new InvalidFileException(
                String.format("Chỉ được upload tối đa %d ảnh cho mỗi đánh giá. Hiện tại có %d ảnh, bạn đang cố upload thêm %d ảnh.",
                    MAX_IMAGES_PER_REVIEW, existingCount, files.size())
            );
        }
        
        List<ReviewImage> savedImages = new ArrayList<>();
        int displayOrder = existingCount;
        
        for (MultipartFile file : files) {
            try {
                // Comprehensive file validation
                validateFile(file);
                
                // Validate file signature (magic bytes)
                validateFileSignature(file);
                
                // Read and validate image
                BufferedImage image = readAndValidateImage(file);
                
                // Validate image dimensions
                validateImageDimensions(image.getWidth(), image.getHeight());
                
                // Save file to disk
                String fileName = generateFileName(file.getOriginalFilename());
                Path filePath = saveFile(file, fileName);
                
                // Create and save entity
                ReviewImage reviewImage = ReviewImage.builder()
                    .review(review)
                    .imageUrl("/uploads/review-images/" + fileName)
                    .imagePath(filePath.toString())
                    .fileSize(file.getSize())
                    .width(image.getWidth())
                    .height(image.getHeight())
                    .displayOrder(displayOrder++)
                    .uploadDate(LocalDateTime.now())
                    .build();
                
                ReviewImage saved = reviewImageRepository.save(reviewImage);
                savedImages.add(saved);
                
                log.info("Successfully uploaded image: {} for review ID: {}", fileName, reviewId);
                
            } catch (IOException e) {
                log.error("IO error while uploading image: {}", e.getMessage(), e);
                throw new InvalidFileException("Lỗi khi xử lý file: " + file.getOriginalFilename(), e);
            }
        }
        
        log.info("Successfully uploaded {} images for review ID: {}", savedImages.size(), reviewId);
        return savedImages;
    }
    
    /**
     * Comprehensive file validation
     * Validates: empty check, size, extension, MIME type, executable check
     */
    private void validateFile(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new InvalidFileException("File không được trống");
        }
        
        // Check file size
        if (file.getSize() > maxFileSize) {
            throw new InvalidFileException(
                String.format("Kích thước file '%s' vượt quá giới hạn %d MB. Kích thước hiện tại: %.2f MB",
                    file.getOriginalFilename(),
                    maxFileSize / 1024 / 1024,
                    file.getSize() / 1024.0 / 1024.0)
            );
        }
        
        // Validate filename
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new InvalidFileException("Tên file không hợp lệ");
        }
        
        // Check for path traversal attempts
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new InvalidFileException("Tên file chứa ký tự không hợp lệ");
        }
        
        // Get and validate extension
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (extension.isEmpty()) {
            throw new InvalidFileException("File phải có phần mở rộng");
        }
        
        // Check for blocked executable extensions (SECURITY)
        if (BLOCKED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException(
                String.format("File thực thi không được phép. Phần mở rộng '%s' bị chặn vì lý do bảo mật.", extension)
            );
        }
        
        // Check if extension is allowed
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException(
                String.format("Định dạng file '%s' không được hỗ trợ. Chỉ chấp nhận: %s",
                    extension, String.join(", ", ALLOWED_EXTENSIONS))
            );
        }
        
        // Validate MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidFileException(
                String.format("Loại MIME '%s' không hợp lệ. Chỉ chấp nhận: %s",
                    contentType, String.join(", ", ALLOWED_MIME_TYPES))
            );
        }
    }
    
    /**
     * Validate file signature (magic bytes) to ensure file is actually an image
     * This prevents file type spoofing attacks
     */
    private void validateFileSignature(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12]; // Read first 12 bytes
            int bytesRead = is.read(header);
            
            if (bytesRead < 4) {
                throw new InvalidFileException("File quá nhỏ hoặc bị hỏng");
            }
            
            // Check JPEG signature
            if (header[0] == JPEG_SIGNATURE[0] && 
                header[1] == JPEG_SIGNATURE[1] && 
                header[2] == JPEG_SIGNATURE[2]) {
                return; // Valid JPEG
            }
            
            // Check PNG signature
            if (header[0] == PNG_SIGNATURE[0] && 
                header[1] == PNG_SIGNATURE[1] && 
                header[2] == PNG_SIGNATURE[2] && 
                header[3] == PNG_SIGNATURE[3]) {
                return; // Valid PNG
            }
            
            // Check WebP signature (RIFF....WEBP)
            if (header[0] == WEBP_SIGNATURE[0] && 
                header[1] == WEBP_SIGNATURE[1] && 
                header[2] == WEBP_SIGNATURE[2] && 
                header[3] == WEBP_SIGNATURE[3] &&
                bytesRead >= 12 &&
                header[8] == 0x57 && header[9] == 0x45 && 
                header[10] == 0x42 && header[11] == 0x50) {
                return; // Valid WebP
            }
            
            throw new InvalidFileException(
                "File không phải là ảnh hợp lệ. Chữ ký file (magic bytes) không khớp với JPEG, PNG hoặc WebP."
            );
            
        } catch (IOException e) {
            log.error("Error reading file signature: {}", e.getMessage(), e);
            throw new InvalidFileException("Không thể đọc file để kiểm tra chữ ký", e);
        }
    }
    
    /**
     * Read image file and validate it's a valid image
     */
    private BufferedImage readAndValidateImage(MultipartFile file) throws IOException {
        BufferedImage image = ImageIO.read(file.getInputStream());
        
        if (image == null) {
            throw new InvalidFileException(
                "File không phải là ảnh hợp lệ hoặc định dạng ảnh không được hỗ trợ bởi hệ thống"
            );
        }
        
        return image;
    }
    
    /**
     * Validate image dimensions are within acceptable range
     */
    private void validateImageDimensions(int width, int height) {
        if (width < MIN_WIDTH || height < MIN_HEIGHT) {
            throw new InvalidFileException(
                String.format("Kích thước ảnh quá nhỏ. Tối thiểu: %dx%d pixels. Kích thước hiện tại: %dx%d pixels",
                    MIN_WIDTH, MIN_HEIGHT, width, height)
            );
        }
        
        if (width > MAX_WIDTH || height > MAX_HEIGHT) {
            throw new InvalidFileException(
                String.format("Kích thước ảnh quá lớn. Tối đa: %dx%d pixels. Kích thước hiện tại: %dx%d pixels",
                    MAX_WIDTH, MAX_HEIGHT, width, height)
            );
        }
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
     * Save file to disk
     */
    private Path saveFile(MultipartFile file, String fileName) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        
        // Create directory if it doesn't exist
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
        }
        
        Path filePath = uploadPath.resolve(fileName);
        
        // Save file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        log.debug("Saved file to: {}", filePath.toAbsolutePath());
        return filePath;
    }
    
    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }
    
    /**
     * Get all images for a review
     */
    @Transactional(readOnly = true)
    public List<ReviewImage> getImagesByReviewId(Long reviewId) {
        log.debug("Fetching images for review ID: {}", reviewId);
        return reviewImageRepository.findByReviewIdOrderByDisplayOrderAsc(reviewId);
    }
    
    /**
     * Get all images for a product (from all reviews)
     */
    @Transactional(readOnly = true)
    public List<ReviewImage> getImagesByProductId(Long productId) {
        log.debug("Fetching images for product ID: {}", productId);
        return reviewImageRepository.findByProductIdOrderByUploadDateDesc(productId);
    }
    
    /**
     * Delete an image by ID
     * Removes both the database record and the physical file
     */
    public void deleteImage(Long imageId) {
        log.info("Deleting image with ID: {}", imageId);
        
        ReviewImage image = reviewImageRepository.findById(imageId)
            .orElseThrow(() -> new ResourceNotFoundException("Ảnh không tồn tại với ID: " + imageId));
        
        // Delete physical file from disk
        try {
            Path filePath = Paths.get(image.getImagePath());
            boolean deleted = Files.deleteIfExists(filePath);
            
            if (deleted) {
                log.info("Successfully deleted file: {}", filePath);
            } else {
                log.warn("File not found on disk: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Error deleting file from disk: {}", e.getMessage(), e);
            // Continue with database deletion even if file deletion fails
        }
        
        // Delete from database
        reviewImageRepository.delete(image);
        log.info("Successfully deleted image record from database: {}", imageId);
    }
    
    /**
     * Delete all images for a review
     */
    public void deleteImagesByReviewId(Long reviewId) {
        log.info("Deleting all images for review ID: {}", reviewId);
        
        List<ReviewImage> images = reviewImageRepository.findByReviewIdOrderByDisplayOrderAsc(reviewId);
        
        for (ReviewImage image : images) {
            deleteImage(image.getId());
        }
        
        log.info("Successfully deleted {} images for review ID: {}", images.size(), reviewId);
    }
    
    /**
     * Get image count for a review
     */
    @Transactional(readOnly = true)
    public int getImageCount(Long reviewId) {
        return reviewImageRepository.countByReviewId(reviewId);
    }
}
