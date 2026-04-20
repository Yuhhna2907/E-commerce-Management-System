package com.codegym.smartphonemanagement.controller.admin;

import com.codegym.smartphonemanagement.service.admin.AdminUserService;
import com.codegym.smartphonemanagement.service.admin.dto.AdminUserDTO;
import com.codegym.smartphonemanagement.service.admin.dto.UserStatsDTO;
import com.codegym.smartphonemanagement.service.admin.dto.UserProfileDTO;
import com.codegym.smartphonemanagement.service.admin.dto.AdminLoyaltyAdjustReqDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public String showUserCRM(Model model) {
        UserStatsDTO stats = adminUserService.getUserStats();
        List<AdminUserDTO> users = adminUserService.getAllStandardUsers();
        
        model.addAttribute("stats", stats);
        model.addAttribute("users", users);
        model.addAttribute("pageTitle", "user");
        
        return "admin/user/index";
    }

    @PostMapping("/{id}/toggle-lock")
    public String toggleUserLock(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            boolean isLocked = adminUserService.toggleUserLock(id);
            if (isLocked) {
                redirectAttributes.addFlashAttribute("success", "Đã KHÓA tài khoản (Ban) ID #" + id);
            } else {
                redirectAttributes.addFlashAttribute("success", "Đã MỞ KHÓA tài khoản (Unban) ID #" + id);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}")
    public String showUserProfile(@PathVariable Long id, Model model) {
        UserProfileDTO profile = adminUserService.getUserProfileDetail(id);
        model.addAttribute("profile", profile);
        model.addAttribute("pageTitle", "user");
        return "admin/user/detail";
    }

    @PostMapping("/{id}/loyalty-adjust")
    public String adjustLoyaltyPoints(@PathVariable Long id, 
                                      @ModelAttribute AdminLoyaltyAdjustReqDTO req,
                                      RedirectAttributes redirectAttributes) {
        try {
            adminUserService.adjustLoyaltyPoints(id, req.getPoints(), req.getReason());
            redirectAttributes.addFlashAttribute("success", "Đã " + (req.getPoints() > 0 ? "CỘNG" : "TRỪ") + " " + Math.abs(req.getPoints()) + " điểm Loyalty thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Điều chỉnh điểm thất bại: " + e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }
}
