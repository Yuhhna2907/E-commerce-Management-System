package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.exception.UnauthorizedException;
import com.codegym.smartphonemanagement.model.dto.*;
import com.codegym.smartphonemanagement.service.qa.ProductQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/qa")
@RequiredArgsConstructor
@Slf4j
public class ProductQuestionController {
    
    private final ProductQuestionService questionService;
    
    // TEMPORARY: Hardcoded user ID cho testing (chưa có Spring Security)
    private static final Long HARDCODED_USER_ID = 1L;
    
    /**
     * Lấy danh sách câu hỏi của sản phẩm với sorting và pagination
     * Endpoint: GET /qa/products/{productId}/questions
     * 
     * @param productId ID của sản phẩm
     * @param sortBy Cách sắp xếp: "recent" (mới nhất) hoặc "helpful" (hữu ích nhất)
     * @param page Số trang (bắt đầu từ 0)
     * @param size Số câu hỏi mỗi trang (mặc định 10)
     * @return JSON response với danh sách câu hỏi và thông tin phân trang
     * 
     * Yêu cầu: 7.1, 10.1, 10.4, 11.1, 11.2, 11.5
     */
    @GetMapping("/products/{productId}/questions")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getQuestions(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "recent") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Fetching questions for product ID: {} with sort: {}, page: {}, size: {}", 
                    productId, sortBy, page, size);
            
            // Get current user ID (có thể null nếu chưa đăng nhập)
            Long currentUserId = getCurrentUserId();
            
            // Get questions with pagination
            Page<QuestionResponseDTO> questionsPage = questionService.getQuestions(
                    productId, sortBy, page, size, currentUserId);
            
            // Build success response
            response.put("success", true);
            response.put("questions", questionsPage.getContent());
            response.put("currentPage", questionsPage.getNumber());
            response.put("totalPages", questionsPage.getTotalPages());
            response.put("totalElements", questionsPage.getTotalElements());
            response.put("hasNext", questionsPage.hasNext());
            response.put("hasPrevious", questionsPage.hasPrevious());
            
            log.debug("Successfully fetched {} questions for product ID: {}", 
                    questionsPage.getContent().size(), productId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching questions for product ID {}: {}", productId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Không thể tải danh sách câu hỏi. Vui lòng thử lại.");
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Tạo câu hỏi mới cho sản phẩm (yêu cầu đăng nhập)
     * Endpoint: POST /qa/products/{productId}/questions
     * 
     * TEMPORARY: Sử dụng hardcoded user ID = 1L
     * 
     * @param productId ID của sản phẩm
     * @param requestDTO DTO chứa nội dung câu hỏi
     * @param bindingResult Kết quả validation
     * @return JSON response với thông tin câu hỏi đã tạo
     * 
     * Yêu cầu: 7.1, 7.2, 7.4
     */
    @PostMapping("/products/{productId}/questions")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createQuestion(
            @PathVariable Long productId,
            @Valid @RequestBody QuestionRequestDTO requestDTO,
            BindingResult bindingResult) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate request
            if (bindingResult.hasErrors()) {
                String errorMessage = bindingResult.getFieldError() != null 
                        ? bindingResult.getFieldError().getDefaultMessage() 
                        : "Dữ liệu không hợp lệ";
                
                log.warn("Validation failed for create question: {}", errorMessage);
                response.put("success", false);
                response.put("message", errorMessage);
                return ResponseEntity.badRequest().body(response);
            }
            
            // Get current user ID (mock user = 1L)
            Long currentUserId = getCurrentUserId();
            
            log.info("Creating question for product ID: {} by user ID: {}", productId, currentUserId);
            
            // Create question
            QuestionResponseDTO questionDTO = questionService.createQuestion(productId, requestDTO, currentUserId);
            
            // Build success response
            response.put("success", true);
            response.put("message", "Câu hỏi của bạn đã được gửi thành công");
            response.put("question", questionDTO);
            
            log.info("Successfully created question ID: {} for product ID: {}", 
                    questionDTO.getId(), productId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized attempt to create question: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument for create question: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error creating question for product ID {}: {}", productId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Không thể tạo câu hỏi. Vui lòng thử lại.");
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Trả lời câu hỏi (chỉ SELLER hoặc ADMIN)
     * Endpoint: POST /qa/questions/{questionId}/answers
     * 
     * TEMPORARY: Sử dụng hardcoded user ID = 1L
     * 
     * @param questionId ID của câu hỏi
     * @param requestDTO DTO chứa nội dung câu trả lời
     * @param bindingResult Kết quả validation
     * @return JSON response với thông tin câu trả lời đã tạo
     * 
     * Yêu cầu: 8.1, 8.2, 8.3, 8.4, 8.5
     */
    @PostMapping("/questions/{questionId}/answers")
    @ResponseBody

    public ResponseEntity<Map<String, Object>> answerQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody AnswerRequestDTO requestDTO,
            BindingResult bindingResult) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate request
            if (bindingResult.hasErrors()) {
                String errorMessage = bindingResult.getFieldError() != null 
                        ? bindingResult.getFieldError().getDefaultMessage() 
                        : "Dữ liệu không hợp lệ";
                
                log.warn("Validation failed for answer question: {}", errorMessage);
                response.put("success", false);
                response.put("message", errorMessage);
                return ResponseEntity.badRequest().body(response);
            }
            
            // Get current user ID (mock user = 1L)
            Long currentUserId = getCurrentUserId();
            
            log.info("Answering question ID: {} by user ID: {}", questionId, currentUserId);
            
            // Answer question
            AnswerResponseDTO answerDTO = questionService.answerQuestion(questionId, requestDTO, currentUserId);
            
            // Build success response
            response.put("success", true);
            response.put("message", "Câu trả lời của bạn đã được gửi thành công");
            response.put("answer", answerDTO);
            
            log.info("Successfully created answer ID: {} for question ID: {}", 
                    answerDTO.getId(), questionId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized attempt to answer question: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument for answer question: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error answering question ID {}: {}", questionId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Không thể tạo câu trả lời. Vui lòng thử lại.");
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Bình chọn câu trả lời (yêu cầu đăng nhập)
     * Endpoint: POST /qa/answers/{answerId}/vote
     * 
     * TEMPORARY: Sử dụng hardcoded user ID = 1L
     * 
     * @param answerId ID của câu trả lời
     * @param requestDTO DTO chứa giá trị vote (true = helpful, false = not helpful)
     * @param bindingResult Kết quả validation
     * @return JSON response với thông tin vote đã cập nhật
     * 
     * Yêu cầu: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6
     */
    @PostMapping("/answers/{answerId}/vote")
    @ResponseBody

    public ResponseEntity<Map<String, Object>> voteAnswer(
            @PathVariable Long answerId,
            @Valid @RequestBody VoteRequestDTO requestDTO,
            BindingResult bindingResult) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate request
            if (bindingResult.hasErrors()) {
                String errorMessage = bindingResult.getFieldError() != null 
                        ? bindingResult.getFieldError().getDefaultMessage() 
                        : "Dữ liệu không hợp lệ";
                
                log.warn("Validation failed for vote answer: {}", errorMessage);
                response.put("success", false);
                response.put("message", errorMessage);
                return ResponseEntity.badRequest().body(response);
            }
            
            // Get current user ID (mock user = 1L)
            Long currentUserId = getCurrentUserId();
            
            log.info("Voting on answer ID: {} by user ID: {} - helpful: {}", 
                    answerId, currentUserId, requestDTO.getIsHelpful());
            
            // Vote answer
            AnswerResponseDTO answerDTO = questionService.voteAnswer(answerId, requestDTO, currentUserId);
            
            // Build success response
            response.put("success", true);
            response.put("message", "Cảm ơn bạn đã bình chọn");
            response.put("answer", answerDTO);
            
            log.info("Successfully voted on answer ID: {} by user ID: {}", answerId, currentUserId);
            
            return ResponseEntity.ok(response);
            
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized attempt to vote answer: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument for vote answer: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error voting on answer ID {}: {}", answerId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Không thể bình chọn. Vui lòng thử lại.");
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * TEMPORARY: Lấy ID của user hiện tại (hardcoded = 1L)
     * 
     * @return User ID = 1L
     */
    private Long getCurrentUserId() {
        return HARDCODED_USER_ID;
    }
}
