package com.codegym.smartphonemanagement.service.product.DTO;

import com.codegym.smartphonemanagement.model.ReviewImage;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ReviewResponseDTO {
    private Long id;
    private Long userId;
    private String username;

    private Long productId;
    private Integer rating;
    private String comment;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
    private LocalDateTime createdAt;
    
    private List<ReviewImage> images;
}
