package com.codegym.smartphonemanagement.controller.seller;

import com.codegym.smartphonemanagement.model.Category;
import com.codegym.smartphonemanagement.service.category.ICategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Controller
@RequestMapping("/admin/categories")
public class CategoryController {

    @Autowired
    private ICategoryService categoryService;

    @GetMapping
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("pageTitle", "category");
        return "admin/category/list";
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<?> saveCategory(@ModelAttribute Category category) {
        try {
            categoryService.save(category);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Đã lưu danh mục thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Tên danh mục đã tồn tại!"));
        }
    }

    @PostMapping("/toggle-status/{id}")
    @ResponseBody
    public ResponseEntity<?> toggleStatus(@PathVariable Long id) {
        try {
            categoryService.toggleStatus(id);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            categoryService.delete(id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Xóa thành công!"));
        } catch (RuntimeException e) {
            // Trả về câu báo lỗi "Danh mục đang có sản phẩm..."
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}