package com.codegym.smartphonemanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for handling error pages
 */
@Controller
@RequestMapping("/error")
public class ErrorController {

    /**
     * Handle CSRF error page
     */
    @GetMapping("/csrf")
    public String csrfError(Model model) {
        model.addAttribute("errorTitle", "CSRF Protection");
        model.addAttribute("errorMessage", "Your request could not be processed due to CSRF protection. This usually happens when your session has expired or the page has been open for too long.");
        model.addAttribute("errorSuggestion", "Please refresh the page and try again.");
        return "error/csrf-error";
    }

    /**
     * Handle access denied error page
     */
    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("errorTitle", "Access Denied");
        model.addAttribute("errorMessage", "You do not have permission to access this resource.");
        model.addAttribute("errorSuggestion", "Please contact your administrator if you believe this is an error.");
        return "error/access-denied";
    }
}
