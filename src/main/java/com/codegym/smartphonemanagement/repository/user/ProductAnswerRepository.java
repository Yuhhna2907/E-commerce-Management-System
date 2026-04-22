package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.ProductAnswer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho ProductAnswer entity
 * Hỗ trợ các query methods cho Q&A system
 */
@Repository
public interface ProductAnswerRepository extends JpaRepository<ProductAnswer, Long> {

    /**
     * Tìm tất cả câu trả lời của một câu hỏi
     * Sắp xếp theo thời gian tạo tăng dần (cũ nhất trước)
     */
    List<ProductAnswer> findByQuestionIdOrderByCreatedAtAsc(Long questionId);

    /**
     * Tìm tất cả câu trả lời của một câu hỏi với phân trang
     */
    Page<ProductAnswer> findByQuestionIdOrderByCreatedAtAsc(Long questionId, Pageable pageable);

    /**
     * Tìm câu trả lời của một câu hỏi và sắp xếp theo số vote hữu ích
     * Câu trả lời có nhiều vote "helpful" nhất sẽ hiển thị trước
     */
    @Query("SELECT a FROM ProductAnswer a " +
           "LEFT JOIN a.votes v " +
           "WHERE a.question.id = :questionId " +
           "GROUP BY a.id " +
           "ORDER BY SUM(CASE WHEN v.isHelpful = true THEN 1 ELSE 0 END) DESC, a.createdAt ASC")
    List<ProductAnswer> findByQuestionIdOrderByHelpfulness(@Param("questionId") Long questionId);

    /**
     * Tìm câu trả lời của một câu hỏi và sắp xếp theo số vote hữu ích với phân trang
     */
    @Query("SELECT a FROM ProductAnswer a " +
           "LEFT JOIN a.votes v " +
           "WHERE a.question.id = :questionId " +
           "GROUP BY a.id " +
           "ORDER BY SUM(CASE WHEN v.isHelpful = true THEN 1 ELSE 0 END) DESC, a.createdAt ASC")
    Page<ProductAnswer> findByQuestionIdOrderByHelpfulness(@Param("questionId") Long questionId, Pageable pageable);

    /**
     * Đếm số câu trả lời của một câu hỏi
     */
    long countByQuestionId(Long questionId);

    /**
     * Tìm tất cả câu trả lời của một user (SELLER/ADMIN)
     */
    List<ProductAnswer> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Tìm câu trả lời của một user cho một câu hỏi cụ thể
     */
    Optional<ProductAnswer> findByQuestionIdAndUserId(Long questionId, Long userId);

    /**
     * Tìm tất cả câu trả lời cho các câu hỏi của một sản phẩm
     */
    @Query("SELECT a FROM ProductAnswer a " +
           "WHERE a.question.product.id = :productId " +
           "ORDER BY a.createdAt DESC")
    List<ProductAnswer> findByProductId(@Param("productId") Long productId);

    /**
     * Tìm câu trả lời hữu ích nhất của một câu hỏi
     * (câu trả lời có nhiều vote "helpful" nhất)
     */
    @Query("SELECT a FROM ProductAnswer a " +
           "LEFT JOIN a.votes v " +
           "WHERE a.question.id = :questionId " +
           "GROUP BY a.id " +
           "ORDER BY SUM(CASE WHEN v.isHelpful = true THEN 1 ELSE 0 END) DESC " +
           "LIMIT 1")
    Optional<ProductAnswer> findMostHelpfulAnswerByQuestionId(@Param("questionId") Long questionId);

    /**
     * Đếm tổng số câu trả lời của một user
     */
    long countByUserId(Long userId);

    /**
     * Đếm số câu trả lời của một user cho một sản phẩm cụ thể
     */
    @Query("SELECT COUNT(a) FROM ProductAnswer a " +
           "WHERE a.user.id = :userId " +
           "AND a.question.product.id = :productId")
    long countByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    /**
     * Kiểm tra xem user đã trả lời câu hỏi này chưa
     */
    boolean existsByQuestionIdAndUserId(Long questionId, Long userId);

    /**
     * Tìm các câu trả lời gần đây của một user
     */
    @Query("SELECT a FROM ProductAnswer a " +
           "WHERE a.user.id = :userId " +
           "AND a.createdAt >= CURRENT_TIMESTAMP - :days DAY " +
           "ORDER BY a.createdAt DESC")
    List<ProductAnswer> findRecentAnswersByUserId(@Param("userId") Long userId, @Param("days") int days);

    /**
     * Tìm các câu trả lời có nhiều vote nhất (top N)
     */
    @Query("SELECT a FROM ProductAnswer a " +
           "LEFT JOIN a.votes v " +
           "WHERE a.question.product.id = :productId " +
           "GROUP BY a.id " +
           "ORDER BY SUM(CASE WHEN v.isHelpful = true THEN 1 ELSE 0 END) DESC")
    List<ProductAnswer> findTopAnswersByProductId(@Param("productId") Long productId, Pageable pageable);

    /**
     * Xóa tất cả câu trả lời của một câu hỏi
     */
    void deleteByQuestionId(Long questionId);

    // --- ANALYTICS: Top Responders ---
    @Query("SELECT a.user.username, COUNT(a.id) " +
           "FROM ProductAnswer a " +
           "WHERE a.createdAt >= :startDate AND a.createdAt <= :endDate " +
           "GROUP BY a.user.username " +
           "ORDER BY COUNT(a.id) DESC")
    List<Object[]> findResponderStats(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    // Đếm tổng số vote Helpful/Not Helpful trong hệ thống
    @Query("SELECT SUM(CASE WHEN v.isHelpful = true THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN v.isHelpful = false THEN 1 ELSE 0 END) " +
           "FROM AnswerVote v " +
           "WHERE v.answer.createdAt >= :startDate AND v.answer.createdAt <= :endDate")
    List<Object[]> countHelpfulVotes(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);
}
