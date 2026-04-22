package com.codegym.smartphonemanagement.controller.admin;

import com.codegym.smartphonemanagement.model.AdminNotification;
import com.codegym.smartphonemanagement.service.notification.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @GetMapping
    public String listNotifications(Model model, 
                                   @RequestParam(required = false) String type,
                                   @RequestParam(required = false) Boolean unreadOnly) {
        log.info("Loading Admin Notification Center. Filter: type={}, unreadOnly={}", type, unreadOnly);
        
        List<AdminNotification> notifications = adminNotificationService.getAllNotifications();
        
        // Cụm lọc dữ liệu (Hiện tại làm in-memory cho nhanh, sếp muốn nâng cấp JPA sau cũng được)
        if (type != null && !type.isEmpty() && !type.equalsIgnoreCase("ALL")) {
            notifications = notifications.stream()
                    .filter(n -> n.getType().name().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }
        
        if (unreadOnly != null && unreadOnly) {
            notifications = notifications.stream()
                    .filter(n -> !n.isRead())
                    .collect(Collectors.toList());
        }

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", adminNotificationService.getUnreadCount());
        model.addAttribute("activeFilter", type != null ? type : "ALL");
        model.addAttribute("unreadOnly", unreadOnly != null ? unreadOnly : false);
        model.addAttribute("pageTitle", "notifications");
        
        return "admin/notifications/index";
    }
}
