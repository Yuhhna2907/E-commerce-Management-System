package com.codegym.smartphonemanagement.controller.admin;

import com.codegym.smartphonemanagement.model.ProductQuestion;
import com.codegym.smartphonemanagement.model.dto.AnswerRequestDTO;
import com.codegym.smartphonemanagement.model.dto.AnswerResponseDTO;
import com.codegym.smartphonemanagement.repository.user.ProductQuestionRepository;
import com.codegym.smartphonemanagement.service.qa.ProductQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/qa")
@RequiredArgsConstructor
public class AdminQAController {

    private final ProductQuestionRepository questionRepository;
    private final ProductQuestionService questionService;

    @GetMapping
    public String index(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);
        String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        Page<ProductQuestion> questionPage;
        if ("unanswered".equals(status)) {
            questionPage = questionRepository.findUnansweredForAdmin(kw, pageable);
        } else {
            questionPage = questionRepository.findAllForAdmin(kw, pageable);
        }

        model.addAttribute("questions", questionPage.getContent());
        model.addAttribute("questionPage", questionPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        // Stats
        model.addAttribute("totalQuestions", questionRepository.countAllQuestions());
        model.addAttribute("unansweredCount", questionRepository.countAllUnansweredQuestions());
        model.addAttribute("answeredCount", questionRepository.countAllAnsweredQuestions());

        // Overdue: câu hỏi chưa trả lời đã chờ > 24h
        java.time.LocalDateTime threshold24h = java.time.LocalDateTime.now().minusHours(24);
        model.addAttribute("overdueCount", questionRepository.countOverdueUnansweredQuestions(threshold24h));

        model.addAttribute("pageTitle", "qa");
        return "admin/qa/index";
    }

    @PostMapping("/questions/{questionId}/answer")
    @ResponseBody
    public Map<String, Object> answerQuestion(
            @PathVariable Long questionId,
            @RequestBody Map<String, String> payload) {

        Map<String, Object> response = new HashMap<>();
        try {
            String answerText = payload.get("answerText");
            if (answerText == null || answerText.trim().isEmpty()) {
                throw new IllegalArgumentException("Nội dung câu trả lời không được để trống.");
            }
            AnswerRequestDTO dto = new AnswerRequestDTO();
            dto.setAnswerText(answerText);
            AnswerResponseDTO result = questionService.answerQuestion(questionId, dto);
            response.put("status", "success");
            response.put("message", "Đã trả lời câu hỏi thành công!");
            response.put("answerId", result.getId());
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Lỗi: " + e.getMessage());
        }
        return response;
    }
}
