package com.codegym.smartphonemanagement.controller.seller;

import com.codegym.smartphonemanagement.service.dashboard.DTO.DashboardDTO;
import com.codegym.smartphonemanagement.service.dashboard.seller.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin") // Chỉ để /admin ở đây thôi
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/dashboard") // Đưa /dashboard xuống đây
    public String index(Model model) {
        DashboardDTO stats = dashboardService.getDashboardStats();
        model.addAttribute("stats", stats);
        model.addAttribute("pageTitle", "dashboard");
        return "admin/dashboard/index"; // Đảm bảo file index.html nằm đúng folder này
    }
}