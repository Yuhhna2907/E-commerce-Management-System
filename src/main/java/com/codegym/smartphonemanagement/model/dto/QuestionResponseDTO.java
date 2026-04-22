package com.codegym.smartphonemanagement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO cho response câu hỏi sản phẩm
 * Yêu cầu: 7.2, 8.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponseDTO {
    
    private Long id;
    private Long productId;
    private String questionText;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;
    private List<AnswerResponseDTO> answers;
    private Boolean hasAnswer;
    private Integer totalHelpfulVotes; // Tổng số vote helpful của tất cả answers
    private Integer answerCount; // Số lượng câu trả lời (for JavaScript compatibility)
    private Long userPurchaseCount; // Số lần khách đã mua hàng thành công
    private String tierLabel; // Hạng thành viên (Đồng, Bạc, Vàng, Kim cương)
    private String tierIcon; // Icon hạng thành viên
    private String tierColor; // Màu hạng thành viên
}
