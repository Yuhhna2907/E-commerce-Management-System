package com.codegym.smartphonemanagement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for image processing settings
 */
@Configuration
@ConfigurationProperties(prefix = "app.image")
@Data
public class ImageProcessingConfig {
    
    // Upload settings
    private String uploadDir = "uploads/review-images";
    private String thumbnailDir = "uploads/review-images/thumbnails";
    private long maxFileSize = 5242880; // 5MB
    private int maxImagesPerReview = 5;
    
    // Image dimension constraints
    private int minWidth = 100;
    private int minHeight = 100;
    private int maxWidth = 4096;
    private int maxHeight = 4096;
    
    // Thumbnail settings
    private int thumbnailWidth = 300;
    private int thumbnailHeight = 300;
    private float thumbnailQuality = 0.8f;
    
    // Compression settings
    private float compressionQuality = 0.85f;
    private int maxCompressedWidth = 1920;
    private int maxCompressedHeight = 1920;
    
    // Allowed formats
    private List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "webp");
    private List<String> allowedMimeTypes = List.of("image/jpeg", "image/png", "image/webp");
    
    // Security settings
    private List<String> blockedExtensions = List.of(
        "exe", "bat", "sh", "dll", "so", "cmd", "com", "scr", "vbs", "js", "jar", "app"
    );
    
    // Rate limiting
    private int maxUploadsPerMinute = 10;
    private int maxUploadsPerHour = 50;
}