package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewImage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Review không được null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;
    
    @NotBlank(message = "Image URL không được trống")
    @Size(max = 500)
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;
    
    @NotBlank(message = "Image path không được trống")
    @Size(max = 500)
    @Column(name = "image_path", nullable = false, length = 500)
    private String imagePath;
    
    // Thumbnail support
    @Size(max = 500)
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;
    
    @Size(max = 500)
    @Column(name = "thumbnail_path", length = 500)
    private String thumbnailPath;
    
    @NotNull(message = "File size không được null")
    @Min(value = 0, message = "File size phải >= 0")
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @NotNull(message = "Width không được null")
    @Min(value = 100, message = "Width tối thiểu 100px")
    @Max(value = 4096, message = "Width tối đa 4096px")
    @Column(nullable = false)
    private Integer width;
    
    @NotNull(message = "Height không được null")
    @Min(value = 100, message = "Height tối thiểu 100px")
    @Max(value = 4096, message = "Height tối đa 4096px")
    @Column(nullable = false)
    private Integer height;
    
    // Original dimensions (before compression)
    @Column(name = "original_width")
    private Integer originalWidth;
    
    @Column(name = "original_height")
    private Integer originalHeight;
    
    // Image metadata
    @Size(max = 255)
    @Column(name = "alt_text")
    private String altText;
    
    @Size(max = 50)
    @Column(name = "image_format")
    private String imageFormat;
    
    @Column(name = "color_depth")
    private Integer colorDepth;
    
    @Column(name = "has_transparency")
    private Boolean hasTransparency;
    
    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;
    
    @Min(value = 0, message = "Display order phải >= 0")
    @Column(name = "display_order")
    private Integer displayOrder = 0;
    
    // Processing status
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status")
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;
    
    @Column(name = "processing_error")
    private String processingError;
    
    @PrePersist
    public void prePersist() {
        if (this.uploadDate == null) {
            this.uploadDate = LocalDateTime.now();
        }
        if (this.displayOrder == null) {
            this.displayOrder = 0;
        }
        if (this.processingStatus == null) {
            this.processingStatus = ProcessingStatus.PENDING;
        }
    }
    
    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
