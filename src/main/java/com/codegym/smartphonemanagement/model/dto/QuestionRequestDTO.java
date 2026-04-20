package com.codegym.smartphonemanagement.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho request tạo câu hỏi sản phẩm
 * Yêu cầu: 7.2, 8.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequestDTO {
    
    @NotBlank(message = "Nội dung câu hỏi không được trống")
    @Size(min = 10, max = 500, message = "Câu hỏi phải có độ dài từ 10 đến 500 ký tự")
    private String questionText;
}
