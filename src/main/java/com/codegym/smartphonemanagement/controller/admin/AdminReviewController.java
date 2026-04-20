package com.codegym.smartphonemanagement.controller.admin;

import com.codegym.smartphonemanagement.model.Review;
import com.codegym.smartphonemanagement.service.admin.AdminReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    @GetMapping
    public String index(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        Page<Review> reviewPage = adminReviewService.searchReviews(keyword, rating, page, size);
        
        model.addAttribute("reviews", reviewPage.getContent());
        model.addAttribute("reviewPage", reviewPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("rating", rating);
        model.addAttribute("pageTitle", "review");
        model.addAllAttributes(adminReviewService.getDashboardStats());
        
        return "admin/review/index";
    }

    @PostMapping("/{id}/toggle")
    @ResponseBody
    public Map<String, Object> toggleStatus(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean newStatus = adminReviewService.toggleReviewStatus(id);
            response.put("status", "success");
            response.put("newStatus", newStatus);
            response.put("message", newStatus ? "Đã hiển thị bình luận này." : "Đã ẩn bình luận. Khách hàng sẽ không thấy nữa.");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Lỗi: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/{id}/reply")
    @ResponseBody
    public Map<String, Object> replyToReview(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String replyMessage = payload.get("replyMessage");
            if(replyMessage == null || replyMessage.trim().isEmpty()) {
                throw new IllegalArgumentException("Nội dung phản hồi không được để trống.");
            }
            adminReviewService.replyToReview(id, replyMessage);
            response.put("status", "success");
            response.put("message", "Đã gửi phản hồi thành công!");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Lỗi: " + e.getMessage());
        }
        return response;
    }
}
