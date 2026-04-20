package com.codegym.smartphonemanagement.service.security;

import com.codegym.smartphonemanagement.config.ImageProcessingConfig;
import com.codegym.smartphonemanagement.exception.InvalidFileException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

/**
 * Enhanced file validation service with comprehensive security checks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileValidationService {
    
    private final ImageProcessingConfig config;
    
    // File signature (magic bytes) for validation
    private static final byte[] JPEG_SIGNATURE = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] WEBP_SIGNATURE = new byte[]{0x52, 0x49, 0x46, 0x46}; // "RIFF"
    
    // Known malicious file signatures to block
    private static final List<byte[]> MALICIOUS_SIGNATURES = Arrays.asList(
        new byte[]{0x4D, 0x5A}, // PE executable
        new byte[]{0x7F, 0x45, 0x4C, 0x46}, // ELF executable
        new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}, // Java class file
        new byte[]{0x50, 0x4B, 0x03, 0x04} // ZIP (could contain malicious content)
    );
    
    /**
     * Comprehensive file validation
     */
    public void validateFile(MultipartFile file) {
        validateBasicProperties(file);
        validateFileSignature(file);
        validateImageContent(file);
        validateFileHash(file);
    }
    
    /**
     * Validate basic file properties
     */
    private void validateBasicProperties(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new InvalidFileException("File không được trống");
        }
        
        // Check file size
        if (file.getSize() > config.getMaxFileSize()) {
            throw new InvalidFileException(
                String.format("Kích thước file '%s' vượt quá giới hạn %d MB. Kích thước hiện tại: %.2f MB",
                    file.getOriginalFilename(),
                    config.getMaxFileSize() / 1024 / 1024,
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
        
        // Check for null bytes (security)
        if (originalFilename.contains("\0")) {
            throw new InvalidFileException("Tên file chứa ký tự null không hợp lệ");
        }
        
        // Get and validate extension
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (extension.isEmpty()) {
            throw new InvalidFileException("File phải có phần mở rộng");
        }
        
        // Check for blocked executable extensions (SECURITY)
        if (config.getBlockedExtensions().contains(extension)) {
            throw new InvalidFileException(
                String.format("File thực thi không được phép. Phần mở rộng '%s' bị chặn vì lý do bảo mật.", extension)
            );
        }
        
        // Check if extension is allowed
        if (!config.getAllowedExtensions().contains(extension)) {
            throw new InvalidFileException(
                String.format("Định dạng file '%s' không được hỗ trợ. Chỉ chấp nhận: %s",
                    extension, String.join(", ", config.getAllowedExtensions()))
            );
        }
        
        // Validate MIME type
        String contentType = file.getContentType();
        if (contentType == null || !config.getAllowedMimeTypes().contains(contentType.toLowerCase())) {
            throw new InvalidFileException(
                String.format("Loại MIME '%s' không hợp lệ. Chỉ chấp nhận: %s",
                    contentType, String.join(", ", config.getAllowedMimeTypes()))
            );
        }
    }
    
    /**
     * Validate file signature (magic bytes) with enhanced security checks
     */
    private void validateFileSignature(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[16]; // Read first 16 bytes for more thorough checking
            int bytesRead = is.read(header);
            
            if (bytesRead < 4) {
                throw new InvalidFileException("File quá nhỏ hoặc bị hỏng");
            }
            
            // Check for malicious signatures first
            for (byte[] maliciousSignature : MALICIOUS_SIGNATURES) {
                if (bytesRead >= maliciousSignature.length && 
                    Arrays.equals(Arrays.copyOf(header, maliciousSignature.length), maliciousSignature)) {
                    throw new InvalidFileException("File có chữ ký nguy hiểm và bị từ chối vì lý do bảo mật");
                }
            }
            
            // Check valid image signatures
            boolean isValidImage = false;
            
            // Check JPEG signature
            if (bytesRead >= JPEG_SIGNATURE.length &&
                Arrays.equals(Arrays.copyOf(header, JPEG_SIGNATURE.length), JPEG_SIGNATURE)) {
                isValidImage = true;
            }
            
            // Check PNG signature
            if (bytesRead >= PNG_SIGNATURE.length &&
                Arrays.equals(Arrays.copyOf(header, PNG_SIGNATURE.length), PNG_SIGNATURE)) {
                isValidImage = true;
            }
            
            // Check WebP signature (RIFF....WEBP)
            if (bytesRead >= 12 &&
                Arrays.equals(Arrays.copyOf(header, 4), WEBP_SIGNATURE) &&
                header[8] == 0x57 && header[9] == 0x45 && 
                header[10] == 0x42 && header[11] == 0x50) {
                isValidImage = true;
            }
            
            if (!isValidImage) {
                throw new InvalidFileException(
                    "File không phải là ảnh hợp lệ. Chữ ký file (magic bytes) không khớp với JPEG, PNG hoặc WebP."
                );
            }
            
        } catch (IOException e) {
            log.error("Error reading file signature: {}", e.getMessage(), e);
            throw new InvalidFileException("Không thể đọc file để kiểm tra chữ ký", e);
        }
    }
    
    /**
     * Validate image content by actually reading it
     */
    private void validateImageContent(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            
            if (image == null) {
                throw new InvalidFileException(
                    "File không phải là ảnh hợp lệ hoặc định dạng ảnh không được hỗ trợ bởi hệ thống"
                );
            }
            
            // Validate image dimensions
            validateImageDimensions(image.getWidth(), image.getHeight());
            
            // Check for suspicious image properties
            if (image.getWidth() * image.getHeight() > 50_000_000) { // 50MP limit
                throw new InvalidFileException("Ảnh có độ phân giải quá cao (trên 50 megapixel)");
            }
            
        } catch (IOException e) {
            log.error("Error validating image content: {}", e.getMessage(), e);
            throw new InvalidFileException("Không thể đọc nội dung ảnh", e);
        }
    }
    
    /**
     * Validate image dimensions are within acceptable range
     */
    private void validateImageDimensions(int width, int height) {
        if (width < config.getMinWidth() || height < config.getMinHeight()) {
            throw new InvalidFileException(
                String.format("Kích thước ảnh quá nhỏ. Tối thiểu: %dx%d pixels. Kích thước hiện tại: %dx%d pixels",
                    config.getMinWidth(), config.getMinHeight(), width, height)
            );
        }
        
        if (width > config.getMaxWidth() || height > config.getMaxHeight()) {
            throw new InvalidFileException(
                String.format("Kích thước ảnh quá lớn. Tối đa: %dx%d pixels. Kích thước hiện tại: %dx%d pixels",
                    config.getMaxWidth(), config.getMaxHeight(), width, height)
            );
        }
    }
    
    /**
     * Calculate and validate file hash for integrity
     */
    private void validateFileHash(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(file.getBytes());
            String hashString = HexFormat.of().formatHex(hash);
            
            // Log hash for potential blacklist checking
            log.debug("File hash (SHA-256): {} for file: {}", hashString, file.getOriginalFilename());
            
            // Here you could implement hash-based blacklisting
            // checkHashBlacklist(hashString);
            
        } catch (NoSuchAlgorithmException | IOException e) {
            log.warn("Could not calculate file hash: {}", e.getMessage());
            // Don't fail validation for hash calculation errors
        }
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
}