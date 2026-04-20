package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.model.dto.SaveForLaterResponse;
import com.codegym.smartphonemanagement.service.notification.StockNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/user/stock-notification")
@RequiredArgsConstructor
@Slf4j
public class StockNotificationController {
    
    private final StockNotificationService stockNotificationService;
    
    @PostMapping("/register")
    @ResponseBody
    public ResponseEntity<SaveForLaterResponse> registerStockNotification(
            @RequestParam Long productId,
            @RequestParam(required = false) Long variantId,
            @RequestParam String email) {
        
        log.info("Stock notification registration request: productId={}, variantId={}, email={}", 
                productId, variantId, email);
        
        // Basic email validation
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            return ResponseEntity.badRequest().body(
                SaveForLaterResponse.builder()
                    .success(false)
                    .message("Email không hợp lệ")
                    .build()
            );
        }
        
        SaveForLaterResponse response = stockNotificationService.registerNotification(
                productId, variantId, email.trim());
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}