package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "answer_votes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"answer_id", "user_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ====== Liên kết Answer ======
    @NotNull(message = "Answer không được null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    private ProductAnswer answer;

    // ====== Liên kết User (người vote) ======
    @NotNull(message = "User không được null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ====== Vote type: true = helpful, false = not helpful ======
    @NotNull(message = "Vote type không được null")
    @Column(name = "is_helpful", nullable = false)
    private Boolean isHelpful;

    // ====== Thời gian ======
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // ====== Helper Methods ======

    /**
     * Kiểm tra xem vote này có phải là "hữu ích" không
     */
    public boolean isHelpfulVote() {
        return Boolean.TRUE.equals(isHelpful);
    }

    /**
     * Kiểm tra xem vote này có phải là "không hữu ích" không
     */
    public boolean isNotHelpfulVote() {
        return Boolean.FALSE.equals(isHelpful);
    }

    /**
     * Chuyển đổi vote (từ helpful sang not helpful hoặc ngược lại)
     */
    public void toggleVote() {
        this.isHelpful = !this.isHelpful;
    }

    /**
     * Cập nhật vote type
     */
    public void updateVote(Boolean newIsHelpful) {
        this.isHelpful = newIsHelpful;
    }
}
