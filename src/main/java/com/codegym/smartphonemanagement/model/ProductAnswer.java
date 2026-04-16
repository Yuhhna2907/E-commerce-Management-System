package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ====== Liên kết Question ======
    @NotNull(message = "Question không được null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private ProductQuestion question;

    // ====== Liên kết User (người trả lời - SELLER/ADMIN) ======
    @NotNull(message = "User không được null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ====== Nội dung câu trả lời ======
    @NotBlank(message = "Nội dung câu trả lời không được trống")
    @Size(min = 10, max = 1000, message = "Câu trả lời phải từ 10-1000 ký tự")
    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT")
    private String answerText;

    // ====== Thời gian ======
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ====== Liên kết Votes ======
    @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AnswerVote> votes = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // ====== Helper Methods ======

    /**
     * Đếm số vote "hữu ích"
     */
    public int getHelpfulVoteCount() {
        if (votes == null || votes.isEmpty()) {
            return 0;
        }
        
        return (int) votes.stream()
                .filter(AnswerVote::getIsHelpful)
                .count();
    }

    /**
     * Đếm số vote "không hữu ích"
     */
    public int getNotHelpfulVoteCount() {
        if (votes == null || votes.isEmpty()) {
            return 0;
        }
        
        return (int) votes.stream()
                .filter(vote -> !vote.getIsHelpful())
                .count();
    }

    /**
     * Đếm tổng số vote
     */
    public int getTotalVoteCount() {
        return votes != null ? votes.size() : 0;
    }

    /**
     * Tính tỷ lệ vote hữu ích (0.0 - 1.0)
     */
    public double getHelpfulnessRatio() {
        int total = getTotalVoteCount();
        if (total == 0) {
            return 0.0;
        }
        return (double) getHelpfulVoteCount() / total;
    }

    /**
     * Kiểm tra xem user đã vote chưa
     */
    public boolean hasUserVoted(Long userId) {
        if (votes == null || userId == null) {
            return false;
        }
        
        return votes.stream()
                .anyMatch(vote -> vote.getUser().getId().equals(userId));
    }

    /**
     * Lấy vote của user (nếu có)
     */
    public AnswerVote getUserVote(Long userId) {
        if (votes == null || userId == null) {
            return null;
        }
        
        return votes.stream()
                .filter(vote -> vote.getUser().getId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Thêm vote
     */
    public void addVote(AnswerVote vote) {
        if (votes == null) {
            votes = new ArrayList<>();
        }
        votes.add(vote);
        vote.setAnswer(this);
    }

    /**
     * Xóa vote
     */
    public void removeVote(AnswerVote vote) {
        if (votes != null) {
            votes.remove(vote);
            vote.setAnswer(null);
        }
    }
}
