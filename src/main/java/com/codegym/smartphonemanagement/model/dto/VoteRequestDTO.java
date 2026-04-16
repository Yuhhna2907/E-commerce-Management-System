package com.codegym.smartphonemanagement.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho request bình chọn câu trả lời
 * Yêu cầu: 7.2, 8.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteRequestDTO {
    
    @NotNull(message = "Giá trị bình chọn không được null")
    private Boolean isHelpful;
}
