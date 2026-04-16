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
@Table(name = "product_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ====== Liên kết Product ======
    @NotNull(message = "Product không được null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // ====== Liên kết User (người hỏi) ======
    @NotNull(message = "User không được null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ====== Nội dung câu hỏi ======
    @NotBlank(message = "Nội dung câu hỏi không được trống")
    @Size(min = 10, max = 500, message = "Câu hỏi phải từ 10-500 ký tự")
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    // ====== Thời gian ======
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ====== Liên kết Answers ======
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductAnswer> answers = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // ====== Helper Methods ======

    /**
     * Kiểm tra xem câu hỏi đã được trả lời chưa
     */
    public boolean hasAnswer() {
        return answers != null && !answers.isEmpty();
    }

    /**
     * Lấy số lượng câu trả lời
     */
    public int getAnswerCount() {
        return answers != null ? answers.size() : 0;
    }

    /**
     * Lấy câu trả lời hữu ích nhất (có nhiều vote nhất)
     */
    public ProductAnswer getMostHelpfulAnswer() {
        if (answers == null || answers.isEmpty()) {
            return null;
        }
        
        return answers.stream()
                .max((a1, a2) -> Integer.compare(a1.getHelpfulVoteCount(), a2.getHelpfulVoteCount()))
                .orElse(null);
    }

    /**
     * Thêm câu trả lời
     */
    public void addAnswer(ProductAnswer answer) {
        if (answers == null) {
            answers = new ArrayList<>();
        }
        answers.add(answer);
        answer.setQuestion(this);
    }

    /**
     * Xóa câu trả lời
     */
    public void removeAnswer(ProductAnswer answer) {
        if (answers != null) {
            answers.remove(answer);
            answer.setQuestion(null);
        }
    }
}
