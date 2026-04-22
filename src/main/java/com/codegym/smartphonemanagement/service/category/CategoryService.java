package com.codegym.smartphonemanagement.service.category;

import com.codegym.smartphonemanagement.exception.BadRequestException;
import com.codegym.smartphonemanagement.exception.EntityNotFoundException;
import com.codegym.smartphonemanagement.model.Category;
import com.codegym.smartphonemanagement.model.dto.BrandStatDTO;
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
     * Now includes product count for each category.
     *
     * @return List of all categories with product counts
     */
    @Override
    @Transactional(readOnly = true)
    public List<Category> findAll() {
        List<Object[]> results = categoryRepository.findAllWithProductCounts();
        
        return results.stream()
                .map(row -> {
                    Category category = new Category();
                    category.setId(((Number) row[0]).longValue());
                    category.setName((String) row[1]);
                    category.setActive((Boolean) row[2]);
                    category.setProductCount(((Number) row[3]).longValue());
                    return category;
                })
                .toList();
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
        
        // Populate productCount for each category in the page
        result.getContent().forEach(cat -> {
            cat.setProductCount(categoryRepository.countProductsByCategoryId(cat.getId()));
        });

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
     * @param keyword  Search keyword (can be null or empty for all results)
     * @param active   Filter by active status (null for all)
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

        // Populate productCount for search results
        result.getContent().forEach(cat -> {
            cat.setProductCount(categoryRepository.countProductsByCategoryId(cat.getId()));
        });

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
     * <p>
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
     * <p>
     * Use case: Update validation - check if a category name already exists for another category
     * when updating an existing category's name.
     *
     * @param name The category name to check (will be normalized: trimmed and converted to lowercase)
     * @param id   The category ID to exclude from the check (the category being updated)
     * @return true if another category with this name exists, false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByNameAndIdNot(String name, Long id) {
        String normalizedName = normalizeName(name);
        return categoryRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id);
    }

    // ===== BRAND MANAGEMENT METHODS =====

    @Override
    @Transactional(readOnly = true)
    public List<BrandStatDTO> getBrandStatistics() {
        log.info("Getting brand statistics");

        // Get brand counts from repository
        List<Object[]> brandCounts = categoryRepository.getBrandStatistics();
        Long totalProducts = getTotalProductsCount();

        List<BrandStatDTO> brandStats = brandCounts.stream()
                .map(row -> {
                    String brandName = (String) row[0];
                    Long productCount = (Long) row[1];
                    Double marketShare = totalProducts > 0 ? (productCount.doubleValue() / totalProducts * 100) : 0.0;

                    return BrandStatDTO.builder()
                            .brandName(brandName)
                            .productCount(productCount)
                            .marketShare(Math.round(marketShare * 10.0) / 10.0) // Round to 1 decimal
                            .build();
                })
                .toList();

        log.info("Found {} brands with statistics", brandStats.size());
        return brandStats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllBrands() {
        log.info("Getting all unique brand names");
        List<String> brands = categoryRepository.findDistinctBrands();
        log.info("Found {} unique brands", brands.size());
        return brands;
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalProductsCount() {
        Long count = categoryRepository.getTotalProductsCount();
        log.info("Total products count: {}", count);
        return count;
    }

    @Override
    public void renameBrand(String oldName, String newName) {
        if (oldName == null || oldName.trim().isEmpty()) {
            throw new BadRequestException("Old brand name cannot be empty");
        }
        if (newName == null || newName.trim().isEmpty()) {
            throw new BadRequestException("New brand name cannot be empty");
        }

        String normalizedOldName = oldName.trim();
        String normalizedNewName = newName.trim();

        if (normalizedOldName.equals(normalizedNewName)) {
            throw new BadRequestException("New brand name must be different from old name");
        }

        // Check if old brand exists
        if (!categoryRepository.existsByBrand(normalizedOldName)) {
            throw new BadRequestException("Brand '" + normalizedOldName + "' does not exist");
        }

        log.info("Renaming brand from '{}' to '{}'", normalizedOldName, normalizedNewName);
        int updatedCount = categoryRepository.renameBrand(normalizedOldName, normalizedNewName);
        log.info("Successfully renamed brand. Updated {} products", updatedCount);
    }

    @Override
    public void mergeBrands(String sourceBrand, String targetBrand) {
        if (sourceBrand == null || sourceBrand.trim().isEmpty()) {
            throw new BadRequestException("Source brand name cannot be empty");
        }
        if (targetBrand == null || targetBrand.trim().isEmpty()) {
            throw new BadRequestException("Target brand name cannot be empty");
        }

        String normalizedSource = sourceBrand.trim();
        String normalizedTarget = targetBrand.trim();

        if (normalizedSource.equals(normalizedTarget)) {
            throw new BadRequestException("Source and target brands cannot be the same");
        }

        // Check if both brands exist
        if (!categoryRepository.existsByBrand(normalizedSource)) {
            throw new BadRequestException("Source brand '" + normalizedSource + "' does not exist");
        }
        if (!categoryRepository.existsByBrand(normalizedTarget)) {
            throw new BadRequestException("Target brand '" + normalizedTarget + "' does not exist");
        }

        log.info("Merging brand '{}' into '{}'", normalizedSource, normalizedTarget);
        int updatedCount = categoryRepository.mergeBrands(normalizedSource, normalizedTarget);
        log.info("Successfully merged brands. Updated {} products", updatedCount);
    }

    @Override
    public void deleteBrand(String brandName) {
        if (brandName == null || brandName.trim().isEmpty()) {
            throw new BadRequestException("Brand name cannot be empty");
        }

        String normalizedBrand = brandName.trim();

        // Check if brand exists
        if (!categoryRepository.existsByBrand(normalizedBrand)) {
            throw new BadRequestException("Brand '" + normalizedBrand + "' does not exist");
        }

        log.info("Deleting brand '{}'", normalizedBrand);
        int updatedCount = categoryRepository.deleteBrand(normalizedBrand);
        log.info("Successfully deleted brand. Updated {} products", updatedCount);
    }

    // ===== BULK OPERATIONS =====

    @Override
    public void bulkActivateCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new BadRequestException("Category IDs list cannot be empty");
        }

        log.info("Bulk activating {} categories", categoryIds.size());
        int updatedCount = categoryRepository.bulkUpdateStatus(categoryIds, true);
        log.info("Successfully activated {} categories", updatedCount);
    }

    @Override
    public void bulkDeactivateCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new BadRequestException("Category IDs list cannot be empty");
        }

        log.info("Bulk deactivating {} categories", categoryIds.size());
        int updatedCount = categoryRepository.bulkUpdateStatus(categoryIds, false);
        log.info("Successfully deactivated {} categories", updatedCount);
    }

    @Override
    public void bulkDeleteCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new BadRequestException("Category IDs list cannot be empty");
        }

        // Check if any categories have active products
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        for (Category category : categories) {
            long activeProductCount = category.getProducts() != null
                    ? category.getProducts().stream()
                    .filter(p -> p.getActive() != null && p.getActive())
                    .count()
                    : 0;

            if (activeProductCount > 0) {
                throw new BadRequestException(
                        String.format("Cannot delete category '%s' because it has %d active products",
                                category.getName(), activeProductCount));
            }
        }

        log.info("Bulk deleting {} categories", categoryIds.size());
        categoryRepository.deleteAllById(categoryIds);
        log.info("Successfully deleted {} categories", categoryIds.size());
    }
}