package com.codegym.smartphonemanagement.controller.seller;

import com.codegym.smartphonemanagement.exception.BadRequestException;
import com.codegym.smartphonemanagement.exception.BusinessException;
import com.codegym.smartphonemanagement.exception.EntityNotFoundException;
import com.codegym.smartphonemanagement.model.Category;
import com.codegym.smartphonemanagement.service.category.ICategoryService;
import com.codegym.smartphonemanagement.model.dto.BrandStatDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final ICategoryService categoryService;

    @GetMapping
    public String listCategories(Model model) {
        log.info("Fetching all categories and brands for admin management view");
        
        // Categories data
        List<Category> categories = categoryService.findAll();
        model.addAttribute("categories", categories);
        
        // Brand statistics
        List<BrandStatDTO> brandStats = categoryService.getBrandStatistics();
        model.addAttribute("brandStats", brandStats);
        
        // Available brands for dropdowns
        List<String> availableBrands = categoryService.getAllBrands();
        model.addAttribute("availableBrands", availableBrands);
        
        // Total products count
        Long totalProducts = categoryService.getTotalProductsCount();
        model.addAttribute("totalProducts", totalProducts);
        
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
            return ResponseEntity.ok(Map.of("status", "success", "message", "Category deleted successfully!"));
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
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", "Unexpected error occurred"));
        }
    }

    @PostMapping("/brands/rename")
    @ResponseBody
    public ResponseEntity<?> renameBrand(@RequestParam String oldName, @RequestParam String newName) {
        try {
            log.info("Attempting to rename brand from '{}' to '{}'", oldName, newName);
            categoryService.renameBrand(oldName, newName);
            log.info("Brand renamed successfully from '{}' to '{}'", oldName, newName);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Brand renamed successfully!"));
        } catch (BadRequestException e) {
            log.warn("Failed to rename brand: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error while renaming brand", e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", "Unexpected error occurred"));
        }
    }

    @PostMapping("/brands/merge")
    @ResponseBody
    public ResponseEntity<?> mergeBrands(@RequestParam String sourceBrand, @RequestParam String targetBrand) {
        try {
            log.info("Attempting to merge brand '{}' into '{}'", sourceBrand, targetBrand);
            categoryService.mergeBrands(sourceBrand, targetBrand);
            log.info("Brands merged successfully: '{}' -> '{}'", sourceBrand, targetBrand);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Brands merged successfully!"));
        } catch (BadRequestException e) {
            log.warn("Failed to merge brands: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error while merging brands", e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", "Unexpected error occurred"));
        }
    }

    @PostMapping("/brands/delete")
    @ResponseBody
    public ResponseEntity<?> deleteBrand(@RequestParam String brandName) {
        try {
            log.info("Attempting to delete brand '{}'", brandName);
            categoryService.deleteBrand(brandName);
            log.info("Brand deleted successfully: '{}'", brandName);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Brand deleted successfully!"));
        } catch (BadRequestException e) {
            log.warn("Failed to delete brand: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error while deleting brand", e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", "Unexpected error occurred"));
        }
    }

    @PostMapping("/bulk-activate")
    @ResponseBody
    public ResponseEntity<?> bulkActivateCategories(@RequestParam List<Long> categoryIds) {
        try {
            log.info("Bulk activating {} categories", categoryIds.size());
            categoryService.bulkActivateCategories(categoryIds);
            log.info("Bulk activation completed for {} categories", categoryIds.size());
            return ResponseEntity.ok(Map.of("status", "success", "message", "Categories activated successfully!"));
        } catch (Exception e) {
            log.error("Unexpected error during bulk activation", e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", "Unexpected error occurred"));
        }
    }

    @PostMapping("/bulk-deactivate")
    @ResponseBody
    public ResponseEntity<?> bulkDeactivateCategories(@RequestParam List<Long> categoryIds) {
        try {
            log.info("Bulk deactivating {} categories", categoryIds.size());
            categoryService.bulkDeactivateCategories(categoryIds);
            log.info("Bulk deactivation completed for {} categories", categoryIds.size());
            return ResponseEntity.ok(Map.of("status", "success", "message", "Categories deactivated successfully!"));
        } catch (Exception e) {
            log.error("Unexpected error during bulk deactivation", e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", "Unexpected error occurred"));
        }
    }

    @PostMapping("/bulk-delete")
    @ResponseBody
    public ResponseEntity<?> bulkDeleteCategories(@RequestParam List<Long> categoryIds) {
        try {
            log.info("Bulk deleting {} categories", categoryIds.size());
            categoryService.bulkDeleteCategories(categoryIds);
            log.info("Bulk deletion completed for {} categories", categoryIds.size());
            return ResponseEntity.ok(Map.of("status", "success", "message", "Categories deleted successfully!"));
        } catch (BadRequestException e) {
            log.warn("Failed to bulk delete categories: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during bulk deletion", e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", "Unexpected error occurred"));
        }
    }
}