package com.codegym.smartphonemanagement.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho request trả lời câu hỏi sản phẩm
 * Yêu cầu: 7.2, 8.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerRequestDTO {
    
    @NotBlank(message = "Nội dung câu trả lời không được trống")
    @Size(min = 10, max = 1000, message = "Câu trả lời phải có độ dài từ 10 đến 1000 ký tự")
    private String answerText;
}
