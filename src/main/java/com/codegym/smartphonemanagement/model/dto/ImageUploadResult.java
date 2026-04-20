package com.codegym.smartphonemanagement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for image upload results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResult {
    private Long id;
    private String imageUrl;
    private String thumbnailUrl;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private Integer displayOrder;
    private LocalDateTime uploadDate;
    private String altText;
    private boolean success;
    private String errorMessage;
}