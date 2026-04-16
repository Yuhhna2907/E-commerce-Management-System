package com.codegym.smartphonemanagement.controller.seller;

import com.codegym.smartphonemanagement.exception.BadRequestException;
import com.codegym.smartphonemanagement.exception.BusinessException;
import com.codegym.smartphonemanagement.exception.EntityNotFoundException;
import com.codegym.smartphonemanagement.model.Category;
import com.codegym.smartphonemanagement.service.category.ICategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final ICategoryService categoryService;

    @GetMapping
    public String listCategories(Model model) {
        log.info("Fetching all categories for admin list view");
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("pageTitle", "category");
        return "admin/category/list";
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<?> saveCategory(@ModelAttribute Category category) {
        try {
            log.info("Attempting to save category: id={}, name={}", category.getId(), category.getName());
            categoryService.save(category);
            log.info("Category saved successfully: id={}, name={}", category.getId(), category.getName());
            return ResponseEntity.ok(Map.of("status", "success", "message", "Đã lưu danh mục thành công!"));
        } catch (BadRequestException e) {
            // Duplicate name or validation error
            log.warn("Failed to save category due to validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        } catch (BusinessException e) {
            // Other business logic errors
            log.error("Business error while saving category: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            // Unexpected errors
            log.error("Unexpected error while saving category", e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", "Đã xảy ra lỗi không mong muốn"));
        }
    }

    @PostMapping("/toggle-status/{id}")
    @ResponseBody
    public ResponseEntity<?> toggleStatus(@PathVariable Long id) {
        try {
            log.info("Toggling status for category id={}", id);
            categoryService.toggleStatus(id);
            log.info("Category status toggled successfully for id={}", id);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (EntityNotFoundException e) {
            // Category not found
            log.warn("Category not found for toggle status: id={}", id);
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        } catch (BusinessException e) {
            // Other business logic errors
            log.error("Business error while toggling category status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            // Unexpected errors
            log.error("Unexpected error while toggling category status", e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", "Đã xảy ra lỗi không mong muốn"));
        }
    }

    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            log.info("Attempting to delete category id={}", id);
            categoryService.delete(id);
            log.info("Category deleted successfully: id={}", id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Xóa thành công!"));
        } catch (BadRequestException e) {
            // Cannot delete category with active products
            log.warn("Cannot delete category: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        } catch (EntityNotFoundException e) {
            // Category not found
            log.warn("Category not found for deletion: id={}", id);
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        } catch (BusinessException e) {
            // Other business logic errors
            log.error("Business error while deleting category: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            // Unexpected errors
            log.error("Unexpected error while deleting category", e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", "Đã xảy ra lỗi không mong muốn"));
        }
    }
}