package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.model.dto.*;
import com.codegym.smartphonemanagement.service.profile.IUserProfileService;
import com.codegym.smartphonemanagement.service.profile.IRecentlyViewedService;
import com.codegym.smartphonemanagement.service.order.user.IOrderService;
import com.codegym.smartphonemanagement.repository.user.ReviewRepository;
import com.codegym.smartphonemanagement.repository.user.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user/profile")
public class UserProfileController {

    private final IUserProfileService profileService;
    private final IRecentlyViewedService recentlyViewedService;
    private final IOrderService orderService;
    private final ReviewRepository reviewRepository;
    private final WishlistRepository wishlistRepository;

    private static final Long USER_ID = 1L; // Thay bằng Security context sau

    // =========================================================
    // GET: Trang profile chính (Tab: Thông tin)
    // =========================================================
    @GetMapping
    public String showProfile(Model model) {
        model.addAttribute("profile", profileService.getProfile(USER_ID));
        model.addAttribute("profileForm", new ProfileUpdateRequestDTO());
        model.addAttribute("passwordForm", new PasswordChangeRequestDTO());
        model.addAttribute("addresses", profileService.getAddresses(USER_ID));
        model.addAttribute("newAddressForm", new UserAddressRequestDTO());
        model.addAttribute("orders", orderService.getOrderHistory(USER_ID));
        model.addAttribute("reviews", reviewRepository.findAll().stream()
                .filter(r -> r.getUser() != null && r.getUser().getId().equals(USER_ID))
                .toList());
        model.addAttribute("wishlists", wishlistRepository.findByUserIdOrderByAddedAtDesc(USER_ID));
        model.addAttribute("recentProducts", recentlyViewedService.getRecentProducts(USER_ID, 20));
        model.addAttribute("activeTab", "info");
        return "user/profile/index";
    }

    // =========================================================
    // POST: Cập nhật thông tin cá nhân
    // =========================================================
    @PostMapping("/update")
    public String updateProfile(@ModelAttribute ProfileUpdateRequestDTO dto,
                                RedirectAttributes redirectAttributes) {
        try {
            profileService.updateProfile(USER_ID, dto);
            redirectAttributes.addFlashAttribute("successMsg", "Cập nhật hồ sơ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/user/profile?tab=info";
    }

    // =========================================================
    // POST: Đổi mật khẩu
    // =========================================================
    @PostMapping("/change-password")
    public String changePassword(@ModelAttribute PasswordChangeRequestDTO dto,
                                 RedirectAttributes redirectAttributes) {
        try {
            profileService.changePassword(USER_ID, dto);
            redirectAttributes.addFlashAttribute("successMsg", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/user/profile?tab=security";
    }

    // =========================================================
    // POST: Thêm địa chỉ mới
    // =========================================================
    @PostMapping("/addresses/add")
    public String addAddress(@ModelAttribute UserAddressRequestDTO dto,
                             RedirectAttributes redirectAttributes) {
        try {
            profileService.addAddress(USER_ID, dto);
            redirectAttributes.addFlashAttribute("successMsg", "Thêm địa chỉ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/user/profile?tab=addresses";
    }

    // =========================================================
    // POST: Đặt làm địa chỉ mặc định
    // =========================================================
    @PostMapping("/addresses/{id}/default")
    public String setDefaultAddress(@PathVariable Long id,
                                    RedirectAttributes redirectAttributes) {
        try {
            profileService.setDefaultAddress(USER_ID, id);
            redirectAttributes.addFlashAttribute("successMsg", "Đã đặt làm địa chỉ mặc định!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/user/profile?tab=addresses";
    }

    // =========================================================
    // POST: Xóa địa chỉ
    // =========================================================
    @PostMapping("/addresses/{id}/delete")
    public String deleteAddress(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        try {
            profileService.deleteAddress(USER_ID, id);
            redirectAttributes.addFlashAttribute("successMsg", "Đã xóa địa chỉ.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/user/profile?tab=addresses";
    }

    // =========================================================
    // POST: Cập nhật địa chỉ
    // =========================================================
    @PostMapping("/addresses/{id}/edit")
    public String updateAddress(@PathVariable Long id,
                                @ModelAttribute UserAddressRequestDTO dto,
                                RedirectAttributes redirectAttributes) {
        try {
            profileService.updateAddress(USER_ID, id, dto);
            redirectAttributes.addFlashAttribute("successMsg", "Cập nhật địa chỉ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/user/profile?tab=addresses";
    }
}
