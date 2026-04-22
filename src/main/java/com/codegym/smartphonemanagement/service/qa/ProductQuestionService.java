package com.codegym.smartphonemanagement.service.qa;

import com.codegym.smartphonemanagement.exception.BadRequestException;
import com.codegym.smartphonemanagement.exception.EntityNotFoundException;
import com.codegym.smartphonemanagement.exception.UnauthorizedAccessException;
import com.codegym.smartphonemanagement.model.*;
import com.codegym.smartphonemanagement.model.dto.*;
import com.codegym.smartphonemanagement.repository.seller.ProductRepository;
import com.codegym.smartphonemanagement.repository.user.*;
import com.codegym.smartphonemanagement.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service xử lý logic cho Q&A System
 * Yêu cầu: 7.1, 7.2, 7.3, 7.4, 7.5, 8.1, 8.2, 8.3, 8.4, 8.5, 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 11.3, 11.4
 * 
 * ✅ Đã tích hợp Spring Security để lấy current user
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductQuestionService {
    
    private final ProductQuestionRepository questionRepository;
    private final ProductAnswerRepository answerRepository;
    private final AnswerVoteRepository voteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final EmailService emailService;
    private final com.codegym.smartphonemanagement.service.notification.AdminNotificationService adminNotificationService;
    
    // ==================== Public Methods ====================
    
    /**
     * Tạo câu hỏi mới cho sản phẩm
     * Yêu cầu: 7.1, 7.2, 7.4
     * 
     * @param productId ID của sản phẩm
     * @param requestDTO DTO chứa nội dung câu hỏi
     * @return QuestionResponseDTO
     * @throws EntityNotFoundException nếu sản phẩm không tồn tại
     * @throws BadRequestException nếu input không hợp lệ
     */
    @Transactional
    public QuestionResponseDTO createQuestion(Long productId, QuestionRequestDTO requestDTO) {
        log.debug("Creating question for product {}", productId);
        
        // Validate inputs
        validateProductId(productId);
        validateQuestionRequest(requestDTO);
        
        // Get current user from Spring Security
        User currentUser = getCurrentUser();
        log.debug("Current user: {} (ID: {})", currentUser.getUsername(), currentUser.getId());
        
        // Validate product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Sản phẩm không tồn tại với ID: " + productId));
        
        log.debug("Found product: {} (ID: {})", product.getName(), product.getId());
        
        // Create question entity
        ProductQuestion question = ProductQuestion.builder()
                .product(product)
                .user(currentUser)
                .questionText(requestDTO.getQuestionText())
                .build();
        
        // Save question
        ProductQuestion savedQuestion = questionRepository.save(question);
        log.info("Question created successfully with ID: {} for product: {}", savedQuestion.getId(), productId);

        // [NEW] Notify Admin
        adminNotificationService.notify(
                "Câu hỏi mới cho: " + product.getName(),
                "Khách hàng " + getUserDisplayName(currentUser) + " vừa hỏi: \"" + requestDTO.getQuestionText() + "\"",
                AdminNotificationType.NEW_QUESTION,
                NotificationPriority.MEDIUM,
                "/admin/qa?id=" + savedQuestion.getId()
        );
        
        // Convert to DTO and return
        return convertToQuestionDTO(savedQuestion, currentUser.getId());
    }
    
    /**
     * Trả lời câu hỏi (chỉ SELLER hoặc ADMIN)
     * Yêu cầu: 8.1, 8.2, 8.3, 8.4, 8.5
     * 
     * @param questionId ID của câu hỏi
     * @param requestDTO DTO chứa nội dung câu trả lời
     * @return AnswerResponseDTO
     * @throws EntityNotFoundException nếu câu hỏi không tồn tại
     * @throws UnauthorizedAccessException nếu user không có quyền trả lời
     * @throws BadRequestException nếu input không hợp lệ
     */
    @Transactional
    public AnswerResponseDTO answerQuestion(Long questionId, AnswerRequestDTO requestDTO) {
        log.debug("Answering question {}", questionId);
        
        // Validate inputs
        validateQuestionId(questionId);
        validateAnswerRequest(requestDTO);
        
        // Get current user from Spring Security
        User currentUser = getCurrentUser();
        
        // Check permission (only SELLER or ADMIN can answer)
        if (!hasAnswerPermission(currentUser)) {
            throw new UnauthorizedAccessException("Chỉ SELLER hoặc ADMIN mới có quyền trả lời câu hỏi");
        }
        
        log.debug("User {} has answer permission", currentUser.getUsername());
        
        // Validate question exists
        ProductQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Câu hỏi không tồn tại với ID: " + questionId));
        
        // Create answer entity
        ProductAnswer answer = ProductAnswer.builder()
                .question(question)
                .user(currentUser)
                .answerText(requestDTO.getAnswerText())
                .build();
        
        // Save answer
        ProductAnswer savedAnswer = answerRepository.save(answer);
        log.info("Answer created successfully with ID: {} for question: {}", savedAnswer.getId(), questionId);
        
        // Gửi email thông báo cho khách hàng
        try {
            if (question.getUser().getEmail() != null) {
                emailService.sendAnswerNotificationEmail(
                        question.getUser().getEmail(),
                        getUserDisplayName(question.getUser()),
                        question.getProduct().getName(),
                        question.getQuestionText(),
                        requestDTO.getAnswerText()
                );
            }
        } catch (Exception e) {
            log.error("Failed to send answer notification email: {}", e.getMessage());
        }

        // Convert to DTO and return
        return convertToAnswerDTO(savedAnswer, currentUser.getId());
    }
    
    /**
     * Bình chọn câu trả lời (helpful hoặc not helpful)
     * Yêu cầu: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6
     * 
     * @param answerId ID của câu trả lời
     * @param requestDTO DTO chứa giá trị vote (true = helpful, false = not helpful)
     * @return AnswerResponseDTO với thông tin vote đã cập nhật
     * @throws EntityNotFoundException nếu câu trả lời không tồn tại
     * @throws BadRequestException nếu input không hợp lệ
     */
    @Transactional
    public AnswerResponseDTO voteAnswer(Long answerId, VoteRequestDTO requestDTO) {
        log.debug("Voting on answer {}: {}", answerId, requestDTO.getIsHelpful());
        
        // Validate inputs
        validateAnswerId(answerId);
        validateVoteRequest(requestDTO);
        
        // Get current user from Spring Security
        User currentUser = getCurrentUser();
        
        // Validate answer exists
        ProductAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new EntityNotFoundException("Câu trả lời không tồn tại với ID: " + answerId));
        
        // Check if user already voted (upsert logic)
        AnswerVote existingVote = voteRepository.findByAnswerIdAndUserId(answerId, currentUser.getId())
                .orElse(null);
        
        if (existingVote != null) {
            // Update existing vote
            existingVote.setIsHelpful(requestDTO.getIsHelpful());
            voteRepository.save(existingVote);
            log.info("Updated existing vote for answer {} by user {}", answerId, currentUser.getId());
        } else {
            // Create new vote
            AnswerVote newVote = AnswerVote.builder()
                    .answer(answer)
                    .user(currentUser)
                    .isHelpful(requestDTO.getIsHelpful())
                    .build();
            voteRepository.save(newVote);
            log.info("Created new vote for answer {} by user {}", answerId, currentUser.getId());
        }
        
        // Reload answer to get updated vote counts
        answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new EntityNotFoundException("Câu trả lời không tồn tại với ID: " + answerId));
        
        // Convert to DTO and return
        return convertToAnswerDTO(answer, currentUser.getId());
    }
    
    /**
     * Lấy danh sách câu hỏi của sản phẩm với sorting và pagination
     * Yêu cầu: 7.1, 7.5, 10.1, 10.4, 11.1, 11.2, 11.5
     * 
     * @param productId ID của sản phẩm
     * @param sortBy Cách sắp xếp: "recent" (mới nhất) hoặc "helpful" (hữu ích nhất)
     * @param page Số trang (bắt đầu từ 0)
     * @param size Số câu hỏi mỗi trang
     * @return Page<QuestionResponseDTO>
     * @throws BadRequestException nếu input không hợp lệ
     */
    @Transactional(readOnly = true)
    public Page<QuestionResponseDTO> getQuestions(Long productId, String sortBy, int page, int size) {
        log.debug("Getting questions for product {} with sort: {}, page: {}, size: {}", productId, sortBy, page, size);
        
        // Validate inputs
        validateProductId(productId);
        validatePagination(page, size);
        
        // Get current user ID (có thể null nếu chưa đăng nhập)
        Long currentUserId = getCurrentUserIdOrNull();
        
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
        return questionsPage.map(question -> convertToQuestionDTO(question, currentUserId));
    }
    
    /**
     * Lấy một câu hỏi cụ thể theo ID
     * 
     * @param questionId ID của câu hỏi
     * @return QuestionResponseDTO
     * @throws EntityNotFoundException nếu câu hỏi không tồn tại
     * @throws BadRequestException nếu input không hợp lệ
     */
    @Transactional(readOnly = true)
    public QuestionResponseDTO getQuestionById(Long questionId) {
        log.debug("Getting question by ID: {}", questionId);
        
        // Validate input
        validateQuestionId(questionId);
        
        // Get current user ID (có thể null nếu chưa đăng nhập)
        Long currentUserId = getCurrentUserIdOrNull();
        
        ProductQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Câu hỏi không tồn tại với ID: " + questionId));
        
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
     * Đếm số câu hỏi của một sản phẩm
     * 
     * @param productId ID của sản phẩm
     * @return Số lượng câu hỏi
     */
    @Transactional(readOnly = true)
    public long countQuestionsByProductId(Long productId) {
        validateProductId(productId);
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
        validateProductId(productId);
        return questionRepository.countUnansweredQuestionsByProductId(productId);
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Lấy current user từ Spring Security context
     * 
     * @return User entity
     * @throws UnauthorizedAccessException nếu user chưa đăng nhập
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedAccessException("Bạn cần đăng nhập để thực hiện thao tác này");
        }
        
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User không tồn tại: " + username));
    }
    
    /**
     * Lấy current user ID từ Spring Security context (có thể null nếu chưa đăng nhập)
     * 
     * @return User ID hoặc null
     */
    private Long getCurrentUserIdOrNull() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getPrincipal())) {
                return null;
            }
            
            String username = authentication.getName();
            return userRepository.findByUsername(username)
                    .map(User::getId)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get current user ID: {}", e.getMessage());
            return null;
        }
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
        log.debug("Converting question ID {} to DTO", question.getId());
        
        // Convert answers to DTOs
        List<AnswerResponseDTO> answerDTOs = convertAnswersToDTO(question.getAnswers(), currentUserId);
        
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
                .userName(getUserDisplayName(question.getUser()))
                .createdAt(question.getCreatedAt())
                .answers(answerDTOs)
                .hasAnswer(!answerDTOs.isEmpty())
                .totalHelpfulVotes(totalHelpfulVotes)
                .answerCount(answerDTOs.size())
                .userPurchaseCount(orderRepository.countByUserId(question.getUser().getId()))
                .tierLabel(MemberTier.BRONZE.getLabel())
                .tierIcon(MemberTier.BRONZE.getIcon())
                .tierColor(MemberTier.BRONZE.getColor())
                .build();

        // Enrich với Loyalty Tier nếu có
        loyaltyAccountRepository.findByUserId(question.getUser().getId()).ifPresent(acc -> {
            MemberTier tier = MemberTier.fromLifetimePoints(acc.getLifetimePoints());
            dto.setTierLabel(tier.getLabel());
            dto.setTierIcon(tier.getIcon());
            dto.setTierColor(tier.getColor());
        });

        return dto;
    }
    
    /**
     * Convert list of ProductAnswer entities sang list of AnswerResponseDTO
     * 
     * @param answers List of ProductAnswer entities
     * @param currentUserId ID của user hiện tại (có thể null)
     * @return List of AnswerResponseDTO
     */
    private List<AnswerResponseDTO> convertAnswersToDTO(List<ProductAnswer> answers, Long currentUserId) {
        if (answers == null || answers.isEmpty()) {
            return new ArrayList<>();
        }
        
        return answers.stream()
                .map(answer -> convertToAnswerDTO(answer, currentUserId))
                .collect(Collectors.toList());
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
        log.debug("Converting answer ID {} to DTO", answer.getId());
        
        // Get vote counts
        int helpfulVotes = answer.getHelpfulVoteCount();
        int notHelpfulVotes = answer.getNotHelpfulVoteCount();
        
        log.debug("Answer ID {} has {} helpful votes, {} not helpful votes", 
                answer.getId(), helpfulVotes, notHelpfulVotes);
        
        // Check current user's vote
        Boolean currentUserVote = getCurrentUserVote(answer.getId(), currentUserId);
        
        // Get user role
        String userRole = getUserRoleDisplay(answer.getUser());
        
        return AnswerResponseDTO.builder()
                .id(answer.getId())
                .answerText(answer.getAnswerText())
                .userId(answer.getUser().getId())
                .userName(getUserDisplayName(answer.getUser()))
                .userRole(userRole)
                .createdAt(answer.getCreatedAt())
                .helpfulVotes(helpfulVotes)
                .notHelpfulVotes(notHelpfulVotes)
                .currentUserVote(currentUserVote)
                .voteCount(helpfulVotes)
                .userVoted(currentUserVote != null)
                .build();
    }
    
    /**
     * Lấy vote của current user cho một answer
     * 
     * @param answerId ID của answer
     * @param currentUserId ID của current user (có thể null)
     * @return Boolean vote value hoặc null
     */
    private Boolean getCurrentUserVote(Long answerId, Long currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        
        return voteRepository.findByAnswerIdAndUserId(answerId, currentUserId)
                .map(AnswerVote::getIsHelpful)
                .orElse(null);
    }
    
    /**
     * Lấy display name của user (fullName hoặc username)
     * 
     * @param user User entity
     * @return Display name
     */
    private String getUserDisplayName(User user) {
        if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
            return user.getFullName();
        }
        return user.getUsername();
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
    
    // ==================== Validation Methods ====================
    
    /**
     * Validate product ID
     */
    private void validateProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw new BadRequestException("Product ID không hợp lệ: " + productId);
        }
    }
    
    /**
     * Validate question ID
     */
    private void validateQuestionId(Long questionId) {
        if (questionId == null || questionId <= 0) {
            throw new BadRequestException("Question ID không hợp lệ: " + questionId);
        }
    }
    
    /**
     * Validate answer ID
     */
    private void validateAnswerId(Long answerId) {
        if (answerId == null || answerId <= 0) {
            throw new BadRequestException("Answer ID không hợp lệ: " + answerId);
        }
    }
    
    /**
     * Validate question request DTO
     */
    private void validateQuestionRequest(QuestionRequestDTO requestDTO) {
        if (requestDTO == null) {
            throw new BadRequestException("Question request không được null");
        }
        if (requestDTO.getQuestionText() == null || requestDTO.getQuestionText().trim().isEmpty()) {
            throw new BadRequestException("Nội dung câu hỏi không được rỗng");
        }
        if (requestDTO.getQuestionText().length() > 500) {
            throw new BadRequestException("Nội dung câu hỏi không được vượt quá 500 ký tự");
        }
    }
    
    /**
     * Validate answer request DTO
     */
    private void validateAnswerRequest(AnswerRequestDTO requestDTO) {
        if (requestDTO == null) {
            throw new BadRequestException("Answer request không được null");
        }
        if (requestDTO.getAnswerText() == null || requestDTO.getAnswerText().trim().isEmpty()) {
            throw new BadRequestException("Nội dung câu trả lời không được rỗng");
        }
        if (requestDTO.getAnswerText().length() > 1000) {
            throw new BadRequestException("Nội dung câu trả lời không được vượt quá 1000 ký tự");
        }
    }
    
    /**
     * Validate vote request DTO
     */
    private void validateVoteRequest(VoteRequestDTO requestDTO) {
        if (requestDTO == null) {
            throw new BadRequestException("Vote request không được null");
        }
        if (requestDTO.getIsHelpful() == null) {
            throw new BadRequestException("Giá trị vote không được null");
        }
    }
    
    /**
     * Validate pagination parameters
     */
    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page number không được âm: " + page);
        }
        if (size <= 0) {
            throw new BadRequestException("Page size phải lớn hơn 0: " + size);
        }
        if (size > 100) {
            throw new BadRequestException("Page size không được vượt quá 100: " + size);
        }
    }


}
