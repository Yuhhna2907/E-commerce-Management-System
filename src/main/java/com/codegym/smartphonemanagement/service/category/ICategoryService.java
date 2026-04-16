package com.codegym.smartphonemanagement.service.category;

import com.codegym.smartphonemanagement.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ICategoryService {
    /**
     * Retrieves all categories without pagination.
     * For backward compatibility with existing code.
     * 
     * @return List of all categories
     */
    List<Category> findAll();
    
    /**
     * Retrieves all categories with pagination and sorting.
     * Supports sorting by name, id, and active status.
     * 
     * @param pageable Pagination parameters (page number, page size, sort)
     * @return Page of categories with metadata (total elements, total pages, current page)
     */
    Page<Category> findAll(Pageable pageable);
    
    Category findById(Long id);
    void save(Category category);
    void delete(Long id);
    void toggleStatus(Long id);
    
    /**
     * Searches categories by keyword with optional active status filter.
     * Search is case-insensitive and matches partial names.
     * 
     * @param keyword Search keyword (can be null or empty for all results)
     * @param active Filter by active status (null for all)
     * @param pageable Pagination parameters (page, size, sort)
     * @return Page of matching categories with metadata
     */
    Page<Category> searchCategories(String keyword, Boolean active, Pageable pageable);
    
    /**
     * Retrieves all active categories without pagination.
     * This is a convenience method for retrieving only categories with active status set to true.
     * 
     * @return List of all active categories
     */
    List<Category> findAllActive();
    
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
    boolean existsByName(String name);
    
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
    boolean existsByNameAndIdNot(String name, Long id);
}