package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.ProductQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho ProductQuestion entity
 * Hỗ trợ các query methods cho Q&A system
 */
@Repository
public interface ProductQuestionRepository extends JpaRepository<ProductQuestion, Long> {

    /**
     * Tìm tất cả câu hỏi của một sản phẩm
     * Sắp xếp theo thời gian tạo giảm dần (mới nhất trước)
     */
    List<ProductQuestion> findByProductIdOrderByCreatedAtDesc(Long productId);

    /**
     * Tìm tất cả câu hỏi của một sản phẩm với phân trang
     * Sắp xếp theo thời gian tạo giảm dần (mới nhất trước)
     */
    Page<ProductQuestion> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    /**
     * Tìm câu hỏi của sản phẩm và sắp xếp theo độ hữu ích
     * Độ hữu ích = số vote "helpful" của câu trả lời có nhiều vote nhất
     * Câu hỏi có câu trả lời hữu ích nhất sẽ hiển thị trước
     */
    @Query("SELECT q FROM ProductQuestion q " +
           "LEFT JOIN q.answers a " +
           "LEFT JOIN a.votes v " +
           "WHERE q.product.id = :productId " +
           "GROUP BY q.id " +
           "ORDER BY SUM(CASE WHEN v.isHelpful = true THEN 1 ELSE 0 END) DESC, q.createdAt DESC")
    Page<ProductQuestion> findByProductIdOrderByHelpfulness(@Param("productId") Long productId, Pageable pageable);

    /**
     * Tìm câu hỏi của sản phẩm và sắp xếp theo độ hữu ích (không phân trang)
     */
    @Query("SELECT q FROM ProductQuestion q " +
           "LEFT JOIN q.answers a " +
           "LEFT JOIN a.votes v " +
           "WHERE q.product.id = :productId " +
           "GROUP BY q.id " +
           "ORDER BY SUM(CASE WHEN v.isHelpful = true THEN 1 ELSE 0 END) DESC, q.createdAt DESC")
    List<ProductQuestion> findByProductIdOrderByHelpfulness(@Param("productId") Long productId);

    /**
     * Đếm số câu hỏi của một sản phẩm
     */
    long countByProductId(Long productId);

    /**
     * Tìm câu hỏi của một user cụ thể
     */
    List<ProductQuestion> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Tìm câu hỏi của một user cho một sản phẩm cụ thể
     */
    List<ProductQuestion> findByProductIdAndUserIdOrderByCreatedAtDesc(Long productId, Long userId);

    /**
     * Tìm các câu hỏi chưa được trả lời của một sản phẩm
     */
    @Query("SELECT q FROM ProductQuestion q " +
           "WHERE q.product.id = :productId " +
           "AND q.answers IS EMPTY " +
           "ORDER BY q.createdAt DESC")
    List<ProductQuestion> findUnansweredQuestionsByProductId(@Param("productId") Long productId);

    /**
     * Tìm các câu hỏi chưa được trả lời của một sản phẩm với phân trang
     */
    @Query("SELECT q FROM ProductQuestion q " +
           "WHERE q.product.id = :productId " +
           "AND q.answers IS EMPTY " +
           "ORDER BY q.createdAt DESC")
    Page<ProductQuestion> findUnansweredQuestionsByProductId(@Param("productId") Long productId, Pageable pageable);

    /**
     * Tìm các câu hỏi đã được trả lời của một sản phẩm
     */
    @Query("SELECT DISTINCT q FROM ProductQuestion q " +
           "JOIN FETCH q.answers " +
           "WHERE q.product.id = :productId " +
           "ORDER BY q.createdAt DESC")
    List<ProductQuestion> findAnsweredQuestionsByProductId(@Param("productId") Long productId);

    /**
     * Đếm số câu hỏi chưa được trả lời của một sản phẩm
     */
    @Query("SELECT COUNT(q) FROM ProductQuestion q " +
           "WHERE q.product.id = :productId " +
           "AND q.answers IS EMPTY")
    long countUnansweredQuestionsByProductId(@Param("productId") Long productId);

    /**
     * Tìm các câu hỏi gần đây nhất (trong N ngày gần đây)
     */
    @Query("SELECT q FROM ProductQuestion q " +
           "WHERE q.product.id = :productId " +
           "AND q.createdAt >= CURRENT_TIMESTAMP - :days DAY " +
           "ORDER BY q.createdAt DESC")
    List<ProductQuestion> findRecentQuestionsByProductId(@Param("productId") Long productId, @Param("days") int days);

    /**
     * Kiểm tra xem user đã hỏi câu hỏi cho sản phẩm này chưa
     */
    boolean existsByProductIdAndUserId(Long productId, Long userId);

    // --- DASHBOARD METRICS ---
    @Query("SELECT COUNT(q) FROM ProductQuestion q WHERE q.answers IS EMPTY")
    long countAllUnansweredQuestions();

    @Query("SELECT COUNT(q) FROM ProductQuestion q")
    long countAllQuestions();

    @Query("SELECT COUNT(q) FROM ProductQuestion q WHERE q.answers IS NOT EMPTY")
    long countAllAnsweredQuestions();

    // --- ADMIN SEARCH ---
    @Query("SELECT q FROM ProductQuestion q " +
           "WHERE (:keyword IS NULL OR LOWER(q.questionText) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(q.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(q.product.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY q.createdAt DESC")
    Page<ProductQuestion> findAllForAdmin(
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT q FROM ProductQuestion q " +
           "WHERE q.answers IS EMPTY " +
           "AND (:keyword IS NULL OR LOWER(q.questionText) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(q.user.username) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY q.createdAt DESC")
    Page<ProductQuestion> findUnansweredForAdmin(
            @Param("keyword") String keyword,
            Pageable pageable);

    // Câu hỏi chưa trả lời đã chờ > 24h (overdue)
    @Query("SELECT COUNT(q) FROM ProductQuestion q " +
           "WHERE q.answers IS EMPTY " +
           "AND q.createdAt < :threshold")
    long countOverdueUnansweredQuestions(@Param("threshold") java.time.LocalDateTime threshold);
}
