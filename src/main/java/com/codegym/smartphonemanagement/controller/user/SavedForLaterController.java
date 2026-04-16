package com.codegym.smartphonemanagement.controller.user;


import com.codegym.smartphonemanagement.model.dto.SavedForLaterDTO;
import com.codegym.smartphonemanagement.service.savedforlater.SavedForLaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user/saved")  // Tránh conflict với /user/cart/save-for-later của CartController
public class SavedForLaterController {
    
    private final SavedForLaterService savedForLaterService;
    
    private static final Long USER_ID = 1L; // TODO: Get from Security context
    
    /**
     * Lưu cart item để mua sau (vào bảng saved_for_later)
     */
    @PostMapping("/cart/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveForLater(@RequestParam Long cartItemId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            SavedForLaterDTO savedItem = savedForLaterService.saveForLater(USER_ID, cartItemId);
            
            response.put("success", true);
            response.put("message", "Đã lưu sản phẩm để mua sau");
            response.put("data", savedItem);
            response.put("savedCount", savedForLaterService.countSavedItems(USER_ID));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Chuyển saved item về giỏ hàng
     */
    @PostMapping("/cart/move")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> moveToCart(@RequestParam Long savedItemId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            savedForLaterService.moveToCart(USER_ID, savedItemId);
            
            response.put("success", true);
            response.put("message", "Đã thêm sản phẩm vào giỏ hàng");
            response.put("savedCount", savedForLaterService.countSavedItems(USER_ID));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Lấy danh sách saved items
     */
    @GetMapping("/items")
    @ResponseBody
    public ResponseEntity<List<SavedForLaterDTO>> getSavedItems() {
        List<SavedForLaterDTO> savedItems = savedForLaterService.getSavedItems(USER_ID);
        return ResponseEntity.ok(savedItems);
    }
    
    /**
     * Xóa saved item
     */
    @DeleteMapping("/items/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeSavedItem(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            savedForLaterService.removeSavedItem(USER_ID, id);
            
            response.put("success", true);
            response.put("message", "Đã xóa sản phẩm");
            response.put("savedCount", savedForLaterService.countSavedItems(USER_ID));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
