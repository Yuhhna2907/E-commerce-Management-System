package com.codegym.smartphonemanagement.service.product.DTO;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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

    private LocalDateTime createdAt;
}
