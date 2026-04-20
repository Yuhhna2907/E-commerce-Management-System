package com.codegym.smartphonemanagement.service.image;

import com.codegym.smartphonemanagement.config.ImageProcessingConfig;
import com.codegym.smartphonemanagement.exception.InvalidFileException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Simplified image processing service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageProcessingService {
    
    private final ImageProcessingConfig config;
    
    /**
     * Process and optimize image
     */
    public ProcessedImage processImage(MultipartFile file, String fileName) throws IOException {
        log.debug("Processing image: {}", fileName);
        
        // Read original image
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new InvalidFileException("Cannot read image file");
        }
        
        // Compress if needed
        BufferedImage processedImage = compressImageIfNeeded(originalImage);
        
        // Generate thumbnail
        BufferedImage thumbnail = generateThumbnail(processedImage);
        
        return ProcessedImage.builder()
            .originalImage(originalImage)
            .processedImage(processedImage)
            .thumbnail(thumbnail)
            .originalWidth(originalImage.getWidth())
            .originalHeight(originalImage.getHeight())
            .processedWidth(processedImage.getWidth())
            .processedHeight(processedImage.getHeight())
            .thumbnailWidth(thumbnail.getWidth())
            .thumbnailHeight(thumbnail.getHeight())
            .format("JPEG") // Default format
            .colorDepth(24) // Default color depth
            .hasTransparency(false) // Simplified
            .build();
    }
    
    /**
     * Save processed image and thumbnail to disk
     */
    public SavedImagePaths saveProcessedImage(ProcessedImage processedImage, String fileName) throws IOException {
        // Ensure directories exist
        Path uploadPath = Paths.get(config.getUploadDir());
        Path thumbnailPath = Paths.get(config.getThumbnailDir());
        
        Files.createDirectories(uploadPath);
        Files.createDirectories(thumbnailPath);
        
        // Save main image
        String mainImagePath = saveImage(processedImage.getProcessedImage(), uploadPath, fileName, "jpg");
        
        // Save thumbnail
        String thumbnailFileName = "thumb_" + fileName;
        String thumbnailImagePath = saveImage(processedImage.getThumbnail(), thumbnailPath, thumbnailFileName, "jpg");
        
        return SavedImagePaths.builder()
            .mainImagePath(mainImagePath)
            .thumbnailPath(thumbnailImagePath)
            .mainImageUrl("/uploads/review-images/" + getFileNameWithExtension(fileName, "jpg"))
            .thumbnailUrl("/uploads/review-images/thumbnails/" + getFileNameWithExtension(thumbnailFileName, "jpg"))
            .build();
    }
    
    /**
     * Compress image if it exceeds size limits
     */
    private BufferedImage compressImageIfNeeded(BufferedImage image) throws IOException {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Check if compression is needed
        if (width <= config.getMaxCompressedWidth() && height <= config.getMaxCompressedHeight()) {
            return image; // No compression needed
        }
        
        // Calculate new dimensions maintaining aspect ratio
        double aspectRatio = (double) width / height;
        int newWidth, newHeight;
        
        if (width > height) {
            newWidth = config.getMaxCompressedWidth();
            newHeight = (int) (newWidth / aspectRatio);
        } else {
            newHeight = config.getMaxCompressedHeight();
            newWidth = (int) (newHeight * aspectRatio);
        }
        
        log.debug("Compressing image from {}x{} to {}x{}", width, height, newWidth, newHeight);
        
        return Thumbnails.of(image)
            .size(newWidth, newHeight)
            .outputQuality(config.getCompressionQuality())
            .asBufferedImage();
    }
    
    /**
     * Generate thumbnail
     */
    private BufferedImage generateThumbnail(BufferedImage image) throws IOException {
        return Thumbnails.of(image)
            .size(config.getThumbnailWidth(), config.getThumbnailHeight())
            .outputQuality(config.getThumbnailQuality())
            .asBufferedImage();
    }
    
    /**
     * Save image to disk
     */
    private String saveImage(BufferedImage image, Path directory, String fileName, String format) throws IOException {
        String fullFileName = getFileNameWithExtension(fileName, format);
        Path filePath = directory.resolve(fullFileName);
        
        // Convert BufferedImage to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        
        // Save to disk
        try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray())) {
            Files.copy(bais, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        log.debug("Saved image to: {}", filePath.toAbsolutePath());
        return filePath.toString();
    }
    
    /**
     * Get filename with extension
     */
    private String getFileNameWithExtension(String fileName, String extension) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex != -1) {
            fileName = fileName.substring(0, lastDotIndex);
        }
        return fileName + "." + extension;
    }
    
    // Simple data classes without Lombok for compatibility
    
    public static class ProcessedImage {
        private BufferedImage originalImage;
        private BufferedImage processedImage;
        private BufferedImage thumbnail;
        private int originalWidth;
        private int originalHeight;
        private int processedWidth;
        private int processedHeight;
        private int thumbnailWidth;
        private int thumbnailHeight;
        private String format;
        private int colorDepth;
        private boolean hasTransparency;
        
        public static ProcessedImageBuilder builder() {
            return new ProcessedImageBuilder();
        }
        
        // Getters
        public BufferedImage getOriginalImage() { return originalImage; }
        public BufferedImage getProcessedImage() { return processedImage; }
        public BufferedImage getThumbnail() { return thumbnail; }
        public int getOriginalWidth() { return originalWidth; }
        public int getOriginalHeight() { return originalHeight; }
        public int getProcessedWidth() { return processedWidth; }
        public int getProcessedHeight() { return processedHeight; }
        public int getThumbnailWidth() { return thumbnailWidth; }
        public int getThumbnailHeight() { return thumbnailHeight; }
        public String getFormat() { return format; }
        public int getColorDepth() { return colorDepth; }
        public boolean isHasTransparency() { return hasTransparency; }
        
        public static class ProcessedImageBuilder {
            private ProcessedImage processedImage = new ProcessedImage();
            
            public ProcessedImageBuilder originalImage(BufferedImage originalImage) {
                this.processedImage.originalImage = originalImage;
                return this;
            }
            
            public ProcessedImageBuilder processedImage(BufferedImage processedImage) {
                this.processedImage.processedImage = processedImage;
                return this;
            }
            
            public ProcessedImageBuilder thumbnail(BufferedImage thumbnail) {
                this.processedImage.thumbnail = thumbnail;
                return this;
            }
            
            public ProcessedImageBuilder originalWidth(int originalWidth) {
                this.processedImage.originalWidth = originalWidth;
                return this;
            }
            
            public ProcessedImageBuilder originalHeight(int originalHeight) {
                this.processedImage.originalHeight = originalHeight;
                return this;
            }
            
            public ProcessedImageBuilder processedWidth(int processedWidth) {
                this.processedImage.processedWidth = processedWidth;
                return this;
            }
            
            public ProcessedImageBuilder processedHeight(int processedHeight) {
                this.processedImage.processedHeight = processedHeight;
                return this;
            }
            
            public ProcessedImageBuilder thumbnailWidth(int thumbnailWidth) {
                this.processedImage.thumbnailWidth = thumbnailWidth;
                return this;
            }
            
            public ProcessedImageBuilder thumbnailHeight(int thumbnailHeight) {
                this.processedImage.thumbnailHeight = thumbnailHeight;
                return this;
            }
            
            public ProcessedImageBuilder format(String format) {
                this.processedImage.format = format;
                return this;
            }
            
            public ProcessedImageBuilder colorDepth(int colorDepth) {
                this.processedImage.colorDepth = colorDepth;
                return this;
            }
            
            public ProcessedImageBuilder hasTransparency(boolean hasTransparency) {
                this.processedImage.hasTransparency = hasTransparency;
                return this;
            }
            
            public ProcessedImage build() {
                return this.processedImage;
            }
        }
    }
    
    public static class SavedImagePaths {
        private String mainImagePath;
        private String thumbnailPath;
        private String mainImageUrl;
        private String thumbnailUrl;
        
        public static SavedImagePathsBuilder builder() {
            return new SavedImagePathsBuilder();
        }
        
        // Getters
        public String getMainImagePath() { return mainImagePath; }
        public String getThumbnailPath() { return thumbnailPath; }
        public String getMainImageUrl() { return mainImageUrl; }
        public String getThumbnailUrl() { return thumbnailUrl; }
        
        public static class SavedImagePathsBuilder {
            private SavedImagePaths savedImagePaths = new SavedImagePaths();
            
            public SavedImagePathsBuilder mainImagePath(String mainImagePath) {
                this.savedImagePaths.mainImagePath = mainImagePath;
                return this;
            }
            
            public SavedImagePathsBuilder thumbnailPath(String thumbnailPath) {
                this.savedImagePaths.thumbnailPath = thumbnailPath;
                return this;
            }
            
            public SavedImagePathsBuilder mainImageUrl(String mainImageUrl) {
                this.savedImagePaths.mainImageUrl = mainImageUrl;
                return this;
            }
            
            public SavedImagePathsBuilder thumbnailUrl(String thumbnailUrl) {
                this.savedImagePaths.thumbnailUrl = thumbnailUrl;
                return this;
            }
            
            public SavedImagePaths build() {
                return this.savedImagePaths;
            }
        }
    }
}