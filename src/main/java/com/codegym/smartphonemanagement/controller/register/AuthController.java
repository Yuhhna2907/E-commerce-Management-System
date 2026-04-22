package com.codegym.smartphonemanagement.controller.register;

import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.model.dto.UserRegistrationDTO;
import com.codegym.smartphonemanagement.service.register.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    @GetMapping("/login")
    public String login(HttpServletRequest request, HttpServletResponse response) {
        if (shouldClearAuthState(request)) {
            expireCookie(response, "JSESSIONID");
            expireCookie(response, "remember-me");
        }
        return "register/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new UserRegistrationDTO());
        return "register/register";
    }

    @PostMapping("/register")
    public String doRegister(@ModelAttribute("user") @Valid UserRegistrationDTO user, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "register/register";
        }

        try {
            userService.register(user);
            return "redirect:/login?success=true";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register/register";
        }
    }

    private boolean shouldClearAuthState(HttpServletRequest request) {
        return request.getParameter("session") != null
                || request.getParameter("error") != null
                || request.getParameter("logout") != null
                || request.getParameter("access_denied") != null;
    }

    private void expireCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly("JSESSIONID".equals(cookieName));
        response.addCookie(cookie);
    }
}
