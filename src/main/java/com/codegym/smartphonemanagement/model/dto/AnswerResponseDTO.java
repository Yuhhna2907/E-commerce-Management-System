package com.codegym.smartphonemanagement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO cho response câu trả lời sản phẩm
 * Yêu cầu: 7.2, 8.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResponseDTO {
    
    private Long id;
    private String answerText;
    private Long userId;
    private String userName;
    private String userRole; // SELLER, ADMIN
    private LocalDateTime createdAt;
    private Integer helpfulVotes;
    private Integer notHelpfulVotes;
    private Boolean currentUserVote; // null = chưa vote, true = helpful, false = not helpful
    
    // JavaScript compatibility fields
    private Integer voteCount; // Tổng số vote (helpfulVotes)
    private Boolean userVoted; // User đã vote chưa (currentUserVote != null)
}
