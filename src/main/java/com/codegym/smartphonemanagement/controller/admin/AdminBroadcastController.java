package com.codegym.smartphonemanagement.controller.admin;

import com.codegym.smartphonemanagement.service.notification.BroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/broadcast")
@RequiredArgsConstructor
public class AdminBroadcastController {

    private final BroadcastService broadcastService;

    @GetMapping
    public String showBroadcastForm(Model model) {
        model.addAttribute("pageTitle", "broadcast");
        return "admin/broadcast/form";
    }

    @PostMapping("/send")
    public String sendBroadcast(
            @RequestParam String target,
            @RequestParam(required = false) String specificUsername,
            @RequestParam String subject,
            @RequestParam String message,
            @RequestParam(required = false) String actionLink,
            @RequestParam(required = false, defaultValue = "false") boolean sendAppNotification,
            @RequestParam(required = false, defaultValue = "false") boolean sendEmail,
            RedirectAttributes redirectAttributes) {

        String finalTarget = target;
        if ("SPECIFIC".equals(target) && specificUsername != null && !specificUsername.trim().isEmpty()) {
            finalTarget = "SPECIFIC:" + specificUsername.trim();
        }

        if (!sendAppNotification && !sendEmail) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn ít nhất một phương thức gửi (App Notification / Email)");
            return "redirect:/admin/broadcast";
        }

        if (subject == null || subject.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng nhập đủ Tiêu đề và Nội dung");
            return "redirect:/admin/broadcast";
        }

        // Run broadcast in the background
        broadcastService.executeBroadcast(
                finalTarget,
                subject,
                message,
                actionLink,
                sendAppNotification,
                sendEmail
        );

        redirectAttributes.addFlashAttribute("success", "Hệ thống đã bắt đầu quá trình gửi thông báo. Quá trình này sẽ chạy ngầm dưới nền.");
        return "redirect:/admin/broadcast";
    }
}
