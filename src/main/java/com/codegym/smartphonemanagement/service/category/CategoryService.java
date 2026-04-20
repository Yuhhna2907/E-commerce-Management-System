package com.codegym.smartphonemanagement.service.category;

import com.codegym.smartphonemanagement.exception.BadRequestException;
import com.codegym.smartphonemanagement.exception.EntityNotFoundException;
import com.codegym.smartphonemanagement.model.Category;
import com.codegym.smartphonemanagement.repository.seller.CategorySellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional // Đảm bảo mọi thao tác DB đều nằm trong Transaction
@RequiredArgsConstructor
@Slf4j
public class CategoryService implements ICategoryService {

    private final CategorySellerRepository categoryRepository;

    /**
     * Retrieves all categories without pagination.
     * For backward compatibility with existing code.
     * 
     * @return List of all categories
     */
    @Override
    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }
    
    /**
     * Retrieves all categories with pagination and sorting.
     * Supports sorting by name, id, and active status.
     * 
     * @param pageable Pagination parameters (page number, page size, sort)
     * @return Page of categories with metadata (total elements, total pages, current page)
     */
    @Override
    @Transactional(readOnly = true)
    public Page<Category> findAll(Pageable pageable) {
        log.info("Đang lấy danh mục với phân trang: trang={}, kích thước={}", 
                 pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Category> result = categoryRepository.findAll(pageable);
        
        log.info("Phân trang hoàn tất: tìm thấy {} kết quả trong tổng số {} danh mục", 
                 result.getNumberOfElements(), result.getTotalElements());
        
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Danh mục", id));
    }

    @Override
    public void save(Category category) {
        // Xử lý logic id rỗng từ form gửi lên
        if (category.getId() != null && category.getId() <= 0) {
            category.setId(null);
        }
        
        // Validate unique name
        validateUniqueName(category);
        
        categoryRepository.save(category);
        log.info("Danh mục đã được lưu thành công: id={}, tên={}", 
                 category.getId(), category.getName());
    }

    private void validateUniqueName(Category category) {
        String normalizedName = normalizeName(category.getName());
        boolean isDuplicate;
        
        if (category.getId() == null) {
            // Creating new category
            isDuplicate = categoryRepository.existsByNameIgnoreCase(normalizedName);
        } else {
            // Updating existing category
            isDuplicate = categoryRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, category.getId());
        }
        
        if (isDuplicate) {
            throw new BadRequestException(
                String.format("Tên danh mục '%s' đã tồn tại trong hệ thống", normalizedName))
                .withAdditionalInfo("categoryName", normalizedName)
                .withAdditionalInfo("operation", category.getId() == null ? "create" : "update");
        }
    }
    
    /**
     * Normalizes category name by trimming whitespace.
     * 
     * @param name The name to normalize
     * @return Normalized name
     */
    private String normalizeName(String name) {
        return name != null ? name.trim() : "";
    }

    @Override
    public void delete(Long id) {
        Category category = findById(id);
        
        // FIX #10: Chỉ đếm sản phẩm active
        long activeProductCount = category.getProducts() != null 
            ? category.getProducts().stream()
                .filter(p -> p.getActive() != null && p.getActive())
                .count()
            : 0;
        
        if (activeProductCount > 0) {
            throw new BadRequestException(
                String.format("Không thể xóa danh mục '%s' vì đang có %d sản phẩm đang hoạt động", 
                             category.getName(), activeProductCount))
                .withAdditionalInfo("categoryId", id)
                .withAdditionalInfo("categoryName", category.getName())
                .withAdditionalInfo("activeProductCount", activeProductCount);
        }
        
        categoryRepository.deleteById(id);
        log.info("Danh mục đã được xóa thành công: id={}, tên={}", id, category.getName());
    }

    @Override
    public void toggleStatus(Long id) {
        Category category = findById(id);
        Boolean oldStatus = category.getActive();
        category.setActive(!category.getActive());
        categoryRepository.save(category);
        log.info("Trạng thái danh mục đã được thay đổi: id={}, tên={}, trạng thái cũ={}, trạng thái mới={}", 
                 id, category.getName(), oldStatus, category.getActive());
    }
    
    /**
     * Searches categories by keyword with optional active status filter.
     * Search is case-insensitive and matches partial names.
     * 
     * @param keyword Search keyword (can be null or empty for all results)
     * @param active Filter by active status (null for all)
     * @param pageable Pagination parameters (page, size, sort)
     * @return Page of matching categories with metadata
     */
    @Override
    @Transactional(readOnly = true)
    public Page<Category> searchCategories(String keyword, Boolean active, Pageable pageable) {
        // Sanitize input
        String sanitizedKeyword = keyword != null ? keyword.trim() : "";
        
        log.info("Đang tìm kiếm danh mục: từ khóa='{}', trạng thái={}, trang={}, kích thước={}", 
                 sanitizedKeyword, active, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Category> result;
        if (sanitizedKeyword.isEmpty() && active == null) {
            result = categoryRepository.findAll(pageable);
        } else if (sanitizedKeyword.isEmpty()) {
            result = categoryRepository.findAllByActive(active, pageable);
        } else if (active == null) {
            result = categoryRepository.findByNameContainingIgnoreCase(sanitizedKeyword, pageable);
        } else {
            result = categoryRepository.findByNameContainingIgnoreCaseAndActive(
                sanitizedKeyword, active, pageable);
        }
        
        log.info("Tìm kiếm hoàn tất: tìm thấy {} kết quả trong tổng số {} danh mục", 
                 result.getNumberOfElements(), result.getTotalElements());
        
        return result;
    }
    
    /**
     * Retrieves all active categories without pagination.
     * This is a convenience method for retrieving only categories with active status set to true.
     * 
     * @return List of all active categories
     */
    @Override
    @Transactional(readOnly = true)
    public List<Category> findAllActive() {
        log.info("Đang lấy tất cả danh mục đang hoạt động");
        List<Category> activeCategories = categoryRepository.findAllByActiveTrue();
        log.info("Tìm thấy {} danh mục đang hoạt động", activeCategories.size());
        return activeCategories;
    }
    
    /**
     * Checks if a category name exists in the system (case-insensitive).
     * This method is used for validation when creating new categories to prevent duplicates.
     * The name comparison is case-insensitive and whitespace is trimmed before checking.
     * 
     * Use case: Create validation - check if a category name already exists before creating a new category.
     * 
     * @param name The category name to check (will be normalized: trimmed and converted to lowercase)
     * @return true if a category with this name exists, false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        String normalizedName = normalizeName(name);
        return categoryRepository.existsByNameIgnoreCase(normalizedName);
    }
    
    /**
     * Checks if a category name exists for a different category (case-insensitive).
     * This method is used for validation when updating existing categories to prevent duplicates.
     * The name comparison is case-insensitive and whitespace is trimmed before checking.
     * 
     * Use case: Update validation - check if a category name already exists for another category
     * when updating an existing category's name.
     * 
     * @param name The category name to check (will be normalized: trimmed and converted to lowercase)
     * @param id The category ID to exclude from the check (the category being updated)
     * @return true if another category with this name exists, false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByNameAndIdNot(String name, Long id) {
        String normalizedName = normalizeName(name);
        return categoryRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id);
    }
}