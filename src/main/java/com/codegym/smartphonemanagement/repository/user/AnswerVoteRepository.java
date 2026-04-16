package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.AnswerVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho AnswerVote entity
 * Hỗ trợ các query methods cho voting system trong Q&A
 */
@Repository
public interface AnswerVoteRepository extends JpaRepository<AnswerVote, Long> {

    /**
     * Tìm vote của một user cho một câu trả lời cụ thể
     * Mỗi user chỉ được vote 1 lần cho mỗi câu trả lời
     */
    Optional<AnswerVote> findByAnswerIdAndUserId(Long answerId, Long userId);

    /**
     * Đếm số vote "hữu ích" của một câu trả lời
     */
    long countByAnswerIdAndIsHelpful(Long answerId, Boolean isHelpful);

    /**
     * Đếm tổng số vote của một câu trả lời
     */
    long countByAnswerId(Long answerId);

    /**
     * Tìm tất cả vote của một câu trả lời
     */
    List<AnswerVote> findByAnswerId(Long answerId);

    /**
     * Tìm tất cả vote của một user
     */
    List<AnswerVote> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Kiểm tra xem user đã vote cho câu trả lời này chưa
     */
    boolean existsByAnswerIdAndUserId(Long answerId, Long userId);

    /**
     * Đếm số vote "hữu ích" của tất cả câu trả lời trong một câu hỏi
     */
    @Query("SELECT COUNT(v) FROM AnswerVote v " +
           "WHERE v.answer.question.id = :questionId " +
           "AND v.isHelpful = true")
    long countHelpfulVotesByQuestionId(@Param("questionId") Long questionId);

    /**
     * Đếm số vote "không hữu ích" của tất cả câu trả lời trong một câu hỏi
     */
    @Query("SELECT COUNT(v) FROM AnswerVote v " +
           "WHERE v.answer.question.id = :questionId " +
           "AND v.isHelpful = false")
    long countNotHelpfulVotesByQuestionId(@Param("questionId") Long questionId);

    /**
     * Tìm tất cả vote của các câu trả lời trong một câu hỏi
     */
    @Query("SELECT v FROM AnswerVote v " +
           "WHERE v.answer.question.id = :questionId " +
           "ORDER BY v.createdAt DESC")
    List<AnswerVote> findByQuestionId(@Param("questionId") Long questionId);

    /**
     * Đếm số vote của một user cho các câu trả lời của một sản phẩm
     */
    @Query("SELECT COUNT(v) FROM AnswerVote v " +
           "WHERE v.user.id = :userId " +
           "AND v.answer.question.product.id = :productId")
    long countByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    /**
     * Tìm các vote gần đây của một user
     */
    @Query("SELECT v FROM AnswerVote v " +
           "WHERE v.user.id = :userId " +
           "AND v.createdAt >= CURRENT_TIMESTAMP - :days DAY " +
           "ORDER BY v.createdAt DESC")
    List<AnswerVote> findRecentVotesByUserId(@Param("userId") Long userId, @Param("days") int days);

    /**
     * Lấy thống kê vote cho một câu trả lời
     * Trả về: [helpfulCount, notHelpfulCount]
     */
    @Query("SELECT " +
           "SUM(CASE WHEN v.isHelpful = true THEN 1 ELSE 0 END) as helpfulCount, " +
           "SUM(CASE WHEN v.isHelpful = false THEN 1 ELSE 0 END) as notHelpfulCount " +
           "FROM AnswerVote v " +
           "WHERE v.answer.id = :answerId")
    Object[] getVoteStatsByAnswerId(@Param("answerId") Long answerId);

    /**
     * Lấy danh sách answer IDs mà user đã vote "helpful"
     */
    @Query("SELECT v.answer.id FROM AnswerVote v " +
           "WHERE v.user.id = :userId " +
           "AND v.isHelpful = true")
    List<Long> findHelpfulAnswerIdsByUserId(@Param("userId") Long userId);

    /**
     * Lấy danh sách answer IDs mà user đã vote "not helpful"
     */
    @Query("SELECT v.answer.id FROM AnswerVote v " +
           "WHERE v.user.id = :userId " +
           "AND v.isHelpful = false")
    List<Long> findNotHelpfulAnswerIdsByUserId(@Param("userId") Long userId);

    /**
     * Xóa tất cả vote của một câu trả lời
     */
    void deleteByAnswerId(Long answerId);

    /**
     * Xóa tất cả vote của một user
     */
    void deleteByUserId(Long userId);

    /**
     * Đếm tổng số vote "helpful" của tất cả câu trả lời của một user
     * (để đánh giá độ hữu ích của người trả lời)
     */
    @Query("SELECT COUNT(v) FROM AnswerVote v " +
           "WHERE v.answer.user.id = :userId " +
           "AND v.isHelpful = true")
    long countHelpfulVotesForUserAnswers(@Param("userId") Long userId);

    /**
     * Tính tỷ lệ vote "helpful" của tất cả câu trả lời của một user
     */
    @Query("SELECT " +
           "CAST(SUM(CASE WHEN v.isHelpful = true THEN 1 ELSE 0 END) AS DOUBLE) / COUNT(v) " +
           "FROM AnswerVote v " +
           "WHERE v.answer.user.id = :userId")
    Double calculateHelpfulnessRatioForUser(@Param("userId") Long userId);
}
