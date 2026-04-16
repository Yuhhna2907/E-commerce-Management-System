package com.codegym.smartphonemanagement.service.qa;

import com.codegym.smartphonemanagement.model.*;
import com.codegym.smartphonemanagement.model.dto.*;
import com.codegym.smartphonemanagement.repository.seller.ProductRepository;
import com.codegym.smartphonemanagement.repository.user.AnswerVoteRepository;
import com.codegym.smartphonemanagement.repository.user.ProductAnswerRepository;
import com.codegym.smartphonemanagement.repository.user.ProductQuestionRepository;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service xử lý logic cho Q&A System
 * Yêu cầu: 7.1, 7.2, 7.3, 7.4, 7.5, 8.1, 8.2, 8.3, 8.4, 8.5, 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 11.3, 11.4
 * 
 * NOTE: Đang giả lập user với ID = 1L (không tích hợp Spring Security)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductQuestionService {
    
    private final ProductQuestionRepository questionRepository;
    private final ProductAnswerRepository answerRepository;
    private final AnswerVoteRepository voteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    
    /**
     * Tạo câu hỏi mới cho sản phẩm
     * Yêu cầu: 7.1, 7.2, 7.4
     * 
     * @param productId ID của sản phẩm
     * @param requestDTO DTO chứa nội dung câu hỏi
     * @param userId ID của user (giả lập, mặc định = 1L)
     * @return QuestionResponseDTO
     * @throws IllegalArgumentException nếu sản phẩm không tồn tại
     */
    public QuestionResponseDTO createQuestion(Long productId, QuestionRequestDTO requestDTO, Long userId) {
        log.info("Creating question for product {} by user {}", productId, userId);
        
        try {
            // Get mock user (default userId = 1L)
            User user = userRepository.findById(userId != null ? userId : 1L)
                    .orElseThrow(() -> new IllegalArgumentException("User không tồn tại: " + userId));
            
            log.debug("Found user: {} (ID: {})", user.getUsername(), user.getId());
            
            // Validate product exists
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại: " + productId));
            
            log.debug("Found product: {} (ID: {})", product.getName(), product.getId());
            
            // Create question entity
            ProductQuestion question = ProductQuestion.builder()
                    .product(product)
                    .user(user)
                    .questionText(requestDTO.getQuestionText())
                    .build();
            
            // Save question
            ProductQuestion savedQuestion = questionRepository.save(question);
            log.info("Question created successfully with ID: {}", savedQuestion.getId());
            
            // Convert to DTO and return
            return convertToQuestionDTO(savedQuestion, userId);
        } catch (Exception e) {
            log.error("EXCEPTION in createQuestion: ", e);
            throw e;
        }
    }
    
    /**
     * Trả lời câu hỏi (chỉ SELLER hoặc ADMIN)
     * Yêu cầu: 8.1, 8.2, 8.3, 8.4, 8.5
     * 
     * @param questionId ID của câu hỏi
     * @param requestDTO DTO chứa nội dung câu trả lời
     * @param userId ID của user (giả lập, mặc định = 1L)
     * @return AnswerResponseDTO
     * @throws IllegalArgumentException nếu câu hỏi không tồn tại
     */
    public AnswerResponseDTO answerQuestion(Long questionId, AnswerRequestDTO requestDTO, Long userId) {
        log.info("Answering question {} by user {}", questionId, userId);
        
        // Get mock user (default userId = 1L)
        User user = userRepository.findById(userId != null ? userId : 1L)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại: " + userId));
        
        // Validate question exists
        ProductQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Câu hỏi không tồn tại: " + questionId));
        
        // Create answer entity
        ProductAnswer answer = ProductAnswer.builder()
                .question(question)
                .user(user)
                .answerText(requestDTO.getAnswerText())
                .build();
        
        // Save answer
        ProductAnswer savedAnswer = answerRepository.save(answer);
        log.info("Answer created successfully with ID: {}", savedAnswer.getId());
        
        // Convert to DTO and return
        return convertToAnswerDTO(savedAnswer, userId);
    }
    
    /**
     * Bình chọn câu trả lời (helpful hoặc not helpful)
     * Yêu cầu: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6
     * 
     * @param answerId ID của câu trả lời
     * @param requestDTO DTO chứa giá trị vote (true = helpful, false = not helpful)
     * @param userId ID của user (giả lập, mặc định = 1L)
     * @return AnswerResponseDTO với thông tin vote đã cập nhật
     * @throws IllegalArgumentException nếu câu trả lời không tồn tại hoặc user không tồn tại
     */
    public AnswerResponseDTO voteAnswer(Long answerId, VoteRequestDTO requestDTO, Long userId) {
        log.info("Voting on answer {} by user {}: {}", answerId, userId, requestDTO.getIsHelpful());
        
        // Get mock user (default userId = 1L)
        User user = userRepository.findById(userId != null ? userId : 1L)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại: " + userId));
        
        // Validate answer exists
        ProductAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("Câu trả lời không tồn tại: " + answerId));
        
        // Check if user already voted (upsert logic)
        AnswerVote existingVote = voteRepository.findByAnswerIdAndUserId(answerId, user.getId())
                .orElse(null);
        
        if (existingVote != null) {
            // Update existing vote
            existingVote.setIsHelpful(requestDTO.getIsHelpful());
            voteRepository.save(existingVote);
            log.info("Updated existing vote for answer {} by user {}", answerId, user.getId());
        } else {
            // Create new vote
            AnswerVote newVote = AnswerVote.builder()
                    .answer(answer)
                    .user(user)
                    .isHelpful(requestDTO.getIsHelpful())
                    .build();
            voteRepository.save(newVote);
            log.info("Created new vote for answer {} by user {}", answerId, user.getId());
        }
        
        // Reload answer to get updated vote counts
        answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("Câu trả lời không tồn tại: " + answerId));
        
        // Convert to DTO and return
        return convertToAnswerDTO(answer, user.getId());
    }
    
    /**
     * Lấy danh sách câu hỏi của sản phẩm với sorting và pagination
     * Yêu cầu: 7.1, 7.5, 10.1, 10.4, 11.1, 11.2, 11.5
     * 
     * @param productId ID của sản phẩm
     * @param sortBy Cách sắp xếp: "recent" (mới nhất) hoặc "helpful" (hữu ích nhất)
     * @param page Số trang (bắt đầu từ 0)
     * @param size Số câu hỏi mỗi trang
     * @param currentUserId ID của user hiện tại (có thể null nếu chưa đăng nhập)
     * @return Page<QuestionResponseDTO>
     */
    @Transactional(readOnly = true)
    public Page<QuestionResponseDTO> getQuestions(Long productId, String sortBy, int page, int size, Long currentUserId) {
        log.info("Getting questions for product {} with sort: {}, page: {}, size: {}", productId, sortBy, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductQuestion> questionsPage;
            
            // Sort by recent or helpful
            if ("helpful".equalsIgnoreCase(sortBy)) {
                log.debug("Sorting by helpfulness");
                questionsPage = questionRepository.findByProductIdOrderByHelpfulness(productId, pageable);
            } else {
                // Default to recent
                log.debug("Sorting by recent (createdAt DESC)");
                questionsPage = questionRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
            }
            
            log.info("Found {} questions for product {}", questionsPage.getTotalElements(), productId);
            
            // Convert to DTOs
            Page<QuestionResponseDTO> result = questionsPage.map(question -> convertToQuestionDTO(question, currentUserId));
            
            log.debug("Converted to {} DTOs", result.getContent().size());
            
            return result;
        } catch (Exception e) {
            log.error("EXCEPTION in getQuestions: ", e);
            throw e;
        }
    }
    
    /**
     * Lấy một câu hỏi cụ thể theo ID
     * 
     * @param questionId ID của câu hỏi
     * @param currentUserId ID của user hiện tại (có thể null)
     * @return QuestionResponseDTO
     * @throws IllegalArgumentException nếu câu hỏi không tồn tại
     */
    @Transactional(readOnly = true)
    public QuestionResponseDTO getQuestionById(Long questionId, Long currentUserId) {
        log.info("Getting question by ID: {}", questionId);
        
        ProductQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Câu hỏi không tồn tại: " + questionId));
        
        return convertToQuestionDTO(question, currentUserId);
    }
    
    /**
     * Kiểm tra xem user có quyền trả lời câu hỏi không
     * Chỉ SELLER hoặc ADMIN mới có quyền trả lời
     * Yêu cầu: 8.5
     * 
     * @param user User cần kiểm tra
     * @return true nếu user có role SELLER hoặc ADMIN, false nếu không
     */
    public boolean hasAnswerPermission(User user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        
        return user.getRoles().stream()
                .anyMatch(role -> "ROLE_SELLER".equals(role.getName()) || "ROLE_ADMIN".equals(role.getName()));
    }
    
    /**
     * Convert ProductQuestion entity sang QuestionResponseDTO
     * Yêu cầu: 7.2, 8.3
     * 
     * @param question ProductQuestion entity
     * @param currentUserId ID của user hiện tại (có thể null)
     * @return QuestionResponseDTO
     */
    private QuestionResponseDTO convertToQuestionDTO(ProductQuestion question, Long currentUserId) {
        try {
            log.debug("Converting question ID {} to DTO", question.getId());
            
            // Convert answers to DTOs
            List<AnswerResponseDTO> answerDTOs = new ArrayList<>();
            if (question.getAnswers() != null && !question.getAnswers().isEmpty()) {
                log.debug("Question has {} answers", question.getAnswers().size());
                answerDTOs = question.getAnswers().stream()
                        .map(answer -> convertToAnswerDTO(answer, currentUserId))
                        .collect(Collectors.toList());
            } else {
                log.debug("Question has no answers");
            }
            
            // Calculate total helpful votes across all answers
            int totalHelpfulVotes = answerDTOs.stream()
                    .mapToInt(AnswerResponseDTO::getHelpfulVotes)
                    .sum();
            
            log.debug("Total helpful votes: {}, Answer count: {}", totalHelpfulVotes, answerDTOs.size());
            
            QuestionResponseDTO dto = QuestionResponseDTO.builder()
                    .id(question.getId())
                    .productId(question.getProduct().getId())
                    .questionText(question.getQuestionText())
                    .userId(question.getUser().getId())
                    .userName(question.getUser().getFullName() != null ? 
                              question.getUser().getFullName() : question.getUser().getUsername())
                    .createdAt(question.getCreatedAt())
                    .answers(answerDTOs)
                    .hasAnswer(!answerDTOs.isEmpty())
                    .totalHelpfulVotes(totalHelpfulVotes)
                    .answerCount(answerDTOs.size()) // For JavaScript compatibility
                    .build();
            
            log.debug("Successfully converted question ID {} to DTO", question.getId());
            return dto;
        } catch (Exception e) {
            log.error("EXCEPTION in convertToQuestionDTO for question ID {}: ", question.getId(), e);
            throw e;
        }
    }
    
    /**
     * Convert ProductAnswer entity sang AnswerResponseDTO
     * Yêu cầu: 7.2, 8.3
     * 
     * @param answer ProductAnswer entity
     * @param currentUserId ID của user hiện tại (có thể null)
     * @return AnswerResponseDTO
     */
    private AnswerResponseDTO convertToAnswerDTO(ProductAnswer answer, Long currentUserId) {
        try {
            log.debug("Converting answer ID {} to DTO", answer.getId());
            
            // Get vote counts
            int helpfulVotes = answer.getHelpfulVoteCount();
            int notHelpfulVotes = answer.getNotHelpfulVoteCount();
            
            log.debug("Answer ID {} has {} helpful votes, {} not helpful votes", 
                    answer.getId(), helpfulVotes, notHelpfulVotes);
            
            // Check current user's vote
            Boolean currentUserVote = null;
            if (currentUserId != null) {
                AnswerVote userVote = voteRepository.findByAnswerIdAndUserId(answer.getId(), currentUserId)
                        .orElse(null);
                if (userVote != null) {
                    currentUserVote = userVote.getIsHelpful();
                    log.debug("User {} voted {} on answer {}", currentUserId, currentUserVote, answer.getId());
                }
            }
            
            // Get user role
            String userRole = getUserRoleDisplay(answer.getUser());
            log.debug("Answer user role: {}", userRole);
            
            AnswerResponseDTO dto = AnswerResponseDTO.builder()
                    .id(answer.getId())
                    .answerText(answer.getAnswerText())
                    .userId(answer.getUser().getId())
                    .userName(answer.getUser().getFullName() != null ? 
                              answer.getUser().getFullName() : answer.getUser().getUsername())
                    .userRole(userRole)
                    .createdAt(answer.getCreatedAt())
                    .helpfulVotes(helpfulVotes)
                    .notHelpfulVotes(notHelpfulVotes)
                    .currentUserVote(currentUserVote)
                    .voteCount(helpfulVotes) // For JavaScript compatibility
                    .userVoted(currentUserVote != null) // For JavaScript compatibility
                    .build();
            
            log.debug("Successfully converted answer ID {} to DTO", answer.getId());
            return dto;
        } catch (Exception e) {
            log.error("EXCEPTION in convertToAnswerDTO for answer ID {}: ", answer.getId(), e);
            throw e;
        }
    }
    
    /**
     * Lấy role hiển thị của user (SELLER hoặc ADMIN)
     * 
     * @param user User entity
     * @return String role name (SELLER, ADMIN, hoặc USER)
     */
    private String getUserRoleDisplay(User user) {
        if (user == null || user.getRoles() == null) {
            return "USER";
        }
        
        // Check for ADMIN first (higher priority)
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
        if (isAdmin) {
            return "ADMIN";
        }
        
        // Check for SELLER
        boolean isSeller = user.getRoles().stream()
                .anyMatch(role -> "ROLE_SELLER".equals(role.getName()));
        if (isSeller) {
            return "SELLER";
        }
        
        return "USER";
    }
    
    /**
     * Đếm số câu hỏi của một sản phẩm
     * 
     * @param productId ID của sản phẩm
     * @return Số lượng câu hỏi
     */
    @Transactional(readOnly = true)
    public long countQuestionsByProductId(Long productId) {
        return questionRepository.countByProductId(productId);
    }
    
    /**
     * Đếm số câu hỏi chưa được trả lời của một sản phẩm
     * 
     * @param productId ID của sản phẩm
     * @return Số lượng câu hỏi chưa được trả lời
     */
    @Transactional(readOnly = true)
    public long countUnansweredQuestionsByProductId(Long productId) {
        return questionRepository.countUnansweredQuestionsByProductId(productId);
    }
}
